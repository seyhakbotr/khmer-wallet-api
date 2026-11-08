package com.wallet.api.account;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.wallet.api.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountControllerIT extends AbstractIntegrationTest {

	private static final String USD = """
			{"currency": "USD"}
			""";

	private static final String KHR = """
			{"currency": "KHR"}
			""";

	@Test
	void createsUsdAccount() throws Exception {
		String token = registerAndLogin();

		mockMvc.perform(post("/api/accounts")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(USD))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.accountNumber").value(org.hamcrest.Matchers.matchesPattern("\\d{10}")))
			.andExpect(jsonPath("$.currency").value("USD"))
			.andExpect(jsonPath("$.createdAt").exists());
	}

	@Test
	void createsBothUsdAndKhrAccounts() throws Exception {
		String token = registerAndLogin();
		String auth = "Bearer " + token;

		mockMvc.perform(post("/api/accounts").header(HttpHeaders.AUTHORIZATION, auth)
				.contentType(MediaType.APPLICATION_JSON).content(USD))
			.andExpect(status().isCreated());
		mockMvc.perform(post("/api/accounts").header(HttpHeaders.AUTHORIZATION, auth)
				.contentType(MediaType.APPLICATION_JSON).content(KHR))
			.andExpect(status().isCreated());

		mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, auth))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].currency").value("USD"))
			.andExpect(jsonPath("$[1].currency").value("KHR"));
	}

	@Test
	void rejectsSecondAccountInSameCurrency() throws Exception {
		String token = registerAndLogin();
		String auth = "Bearer " + token;

		mockMvc.perform(post("/api/accounts").header(HttpHeaders.AUTHORIZATION, auth)
				.contentType(MediaType.APPLICATION_JSON).content(USD))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/accounts").header(HttpHeaders.AUTHORIZATION, auth)
				.contentType(MediaType.APPLICATION_JSON).content(USD))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("User already has a USD account"));
	}

	@Test
	void rejectsUnknownCurrency() throws Exception {
		String token = registerAndLogin();

		mockMvc.perform(post("/api/accounts")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"currency": "EUR"}
						"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void requiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/accounts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(USD))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/accounts"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void listsOnlyOwnAccounts() throws Exception {
		String tokenA = registerAndLogin();
		String tokenB = registerAndLogin();

		mockMvc.perform(post("/api/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
				.contentType(MediaType.APPLICATION_JSON).content(USD))
			.andExpect(status().isCreated());
		mockMvc.perform(post("/api/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
				.contentType(MediaType.APPLICATION_JSON).content(KHR))
			.andExpect(status().isCreated());

		mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].currency").value("USD"));

		mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].currency").value("KHR"));
	}
}
