package com.wallet.api.ledger;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import com.wallet.api.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DepositControllerIT extends AbstractIntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void depositsMoneyAndReturnsNewBalance() throws Exception {
		String token = registerAndLogin();
		String accountNumber = createAccount(token, "USD");

		deposit(token, accountNumber, "10.50")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.entryId").isNumber())
			.andExpect(jsonPath("$.accountNumber").value(accountNumber))
			.andExpect(jsonPath("$.currency").value("USD"))
			.andExpect(jsonPath("$.amount").value("10.50"))
			.andExpect(jsonPath("$.balance").value("10.50"))
			.andExpect(jsonPath("$.createdAt").exists());

		assertThat(entryCount(accountNumber)).isEqualTo(1);
		assertThat(entryAmount(accountNumber)).isEqualTo(1_050L);
		assertThat(entryType(accountNumber)).isEqualTo("DEPOSIT");
	}

	@Test
	void depositsAccumulateIntoTheDerivedBalance() throws Exception {
		String token = registerAndLogin();
		String accountNumber = createAccount(token, "USD");

		deposit(token, accountNumber, "10.50")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.balance").value("10.50"));

		deposit(token, accountNumber, "2.25")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.amount").value("2.25"))
			.andExpect(jsonPath("$.balance").value("12.75"));

		assertThat(entryCount(accountNumber)).isEqualTo(2);
	}

	@Test
	void depositsInTheAccountsCurrency() throws Exception {
		String token = registerAndLogin();
		String accountNumber = createAccount(token, "KHR");

		deposit(token, accountNumber, "100.50")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.currency").value("KHR"))
			.andExpect(jsonPath("$.amount").value("100.50"))
			.andExpect(jsonPath("$.balance").value("100.50"));
	}

	@Test
	void unknownAccountReturns404() throws Exception {
		String token = registerAndLogin();

		deposit(token, "0000000000", "10.00")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Account not found"));
	}

	@Test
	void cannotDepositIntoAnotherUsersAccount() throws Exception {
		String ownerToken = registerAndLogin();
		String otherToken = registerAndLogin();
		String accountNumber = createAccount(ownerToken, "USD");

		deposit(otherToken, accountNumber, "10.00")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Account not found"));

		assertThat(entryCount(accountNumber)).isZero();
	}

	@Test
	void invalidAmountsReturn400AndWriteNoEntry() throws Exception {
		String token = registerAndLogin();
		String accountNumber = createAccount(token, "USD");

		List<String> invalidAmounts = List.of(
				"10.555", "0", "0.00", "-5.00", "+5.00", "abc", "1e5",
				"99999999999999999999.99", "1000000.01");

		for (String amount : invalidAmounts) {
			deposit(token, accountNumber, amount)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.amount").exists());
		}

		assertThat(entryCount(accountNumber)).isZero();
	}

	@Test
	void missingOrBlankAmountReturns400AndWritesNoEntry() throws Exception {
		String token = registerAndLogin();
		String accountNumber = createAccount(token, "USD");

		mockMvc.perform(post("/api/accounts/{accountNumber}/deposits", accountNumber)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.amount").exists());

		deposit(token, accountNumber, "   ")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.amount").exists());

		assertThat(entryCount(accountNumber)).isZero();
	}

	@Test
	void requiresAuthentication() throws Exception {
		String token = registerAndLogin();
		String accountNumber = createAccount(token, "USD");

		mockMvc.perform(post("/api/accounts/{accountNumber}/deposits", accountNumber)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": "10.00"}
						"""))
			.andExpect(status().isUnauthorized());

		assertThat(entryCount(accountNumber)).isZero();
	}

	private long entryCount(String accountNumber) {
		return jdbc.queryForObject("select count(*) from ledger_entries where account_id = ?",
				Long.class, accountId(accountNumber));
	}

	private long entryAmount(String accountNumber) {
		return jdbc.queryForObject("select amount_minor from ledger_entries where account_id = ?",
				Long.class, accountId(accountNumber));
	}

	private String entryType(String accountNumber) {
		return jdbc.queryForObject("select entry_type from ledger_entries where account_id = ?",
				String.class, accountId(accountNumber));
	}

	private long accountId(String accountNumber) {
		return jdbc.queryForObject("select id from accounts where account_number = ?",
				Long.class, accountNumber);
	}
}
