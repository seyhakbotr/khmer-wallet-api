package com.wallet.api.rate;

import java.time.Clock;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import com.wallet.api.AbstractIntegrationTest;
import com.wallet.api.common.Money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExchangeRateIT extends AbstractIntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private Clock clock;

	@Test
	void returnsTheLatestRateEffectiveToday() throws Exception {
		String token = registerAndLogin();
		LocalDate today = LocalDate.now(clock);
		java.util.Map<String, Object> expected = jdbc.queryForMap("""
				select rate_minor, effective_from::text as effective_from
				from usd_khr_rate
				where effective_from <= ? order by effective_from desc limit 1
				""", today);

		mockMvc.perform(get("/api/rates/usd-khr").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.pair").value("USD/KHR"))
			.andExpect(jsonPath("$.rate").value(Money.format(((Number) expected.get("rate_minor")).longValue())))
			.andExpect(jsonPath("$.effectiveFrom").value(expected.get("effective_from")));
	}

	@Test
	void adminPublishesTodaysRate() throws Exception {
		String token = adminToken();
		LocalDate today = LocalDate.now(clock);

		putRate(token, "4150.50", today)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rate").value("4150.50"))
			.andExpect(jsonPath("$.effectiveFrom").value(today.toString()));

		mockMvc.perform(get("/api/rates/usd-khr").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rate").value("4150.50"))
			.andExpect(jsonPath("$.effectiveFrom").value(today.toString()));

		assertThat(jdbc.queryForObject("select set_by_user_id from usd_khr_rate where effective_from = ?",
				Long.class, today)).isNotNull();
	}

	@Test
	void adminCorrectsARateWithoutCreatingADuplicateRow() throws Exception {
		String token = adminToken();
		LocalDate today = LocalDate.now(clock);

		putRate(token, "4300.00", today).andExpect(status().isOk());
		putRate(token, "4301.00", today).andExpect(status().isOk());

		assertThat(jdbc.queryForObject("select count(*) from usd_khr_rate where effective_from = ?",
				Long.class, today)).isEqualTo(1);
		mockMvc.perform(get("/api/rates/usd-khr").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rate").value("4301.00"));
	}

	@Test
	void futureRateIsNotCurrentYet() throws Exception {
		String token = adminToken();
		LocalDate today = LocalDate.now(clock);
		LocalDate tomorrow = today.plusDays(1);

		putRate(token, "9999.99", tomorrow).andExpect(status().isOk());

		mockMvc.perform(get("/api/rates/usd-khr").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rate").value(org.hamcrest.Matchers.not("9999.99")))
			.andExpect(jsonPath("$.effectiveFrom").value(org.hamcrest.Matchers.not(tomorrow.toString())));
	}

	@Test
	void normalUserCannotPublishARate() throws Exception {
		String token = registerAndLogin();
		putRate(token, "4150.00", LocalDate.now(clock))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.message").value("Access denied"));
	}

	@Test
	void publishingRequiresAuthentication() throws Exception {
		mockMvc.perform(put("/api/admin/usd-khr-rates")
				.contentType(MediaType.APPLICATION_JSON)
				.content(rateBody("4150.00", LocalDate.now(clock))))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void invalidRatesAndDatesReturn400() throws Exception {
		String token = adminToken();
		LocalDate today = LocalDate.now(clock);

		for (String rate : java.util.List.of("abc", "0", "4150.555", "-1.00")) {
			putRate(token, rate, today)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.rate").exists());
		}

		mockMvc.perform(put("/api/admin/usd-khr-rates")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rate\": \"4150.00\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.effectiveFrom").exists());
	}

	private String adminToken() throws Exception {
		String phone = uniquePhone();
		register(phone, "Admin User", "password123").andExpect(status().isCreated());
		jdbc.update("update users set role = 'ADMIN' where phone = ?", phone);
		return loginAndGetToken(phone, "password123");
	}

	private org.springframework.test.web.servlet.ResultActions putRate(String token, String rate, LocalDate date)
			throws Exception {
		return mockMvc.perform(put("/api/admin/usd-khr-rates")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(rateBody(rate, date)));
	}

	private String rateBody(String rate, LocalDate date) {
		return """
				{"rate": "%s", "effectiveFrom": "%s"}
				""".formatted(rate, date);
	}

}
