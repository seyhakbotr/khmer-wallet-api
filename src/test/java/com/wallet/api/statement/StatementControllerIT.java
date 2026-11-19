package com.wallet.api.statement;

import java.time.Clock;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.wallet.api.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatementControllerIT extends AbstractIntegrationTest {

	@Autowired
	private StatementService statementService;

	@Autowired
	private Clock clock;

	@Test
	void listsStatementsForTheAccountOwner() throws Exception {
		String token = registerAndLogin();
		String account = createAccount(token, "USD");
		deposit(token, account, "10.00").andExpect(status().isCreated());
		statementService.generateDailyStatements(LocalDate.now(clock));

		mockMvc.perform(get("/api/accounts/{accountNumber}/statements", account)
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].accountNumber").value(account))
			.andExpect(jsonPath("$[0].currency").value("USD"))
			.andExpect(jsonPath("$[0].statementDate").value(LocalDate.now(clock).toString()))
			.andExpect(jsonPath("$[0].openingBalance").value("0.00"))
			.andExpect(jsonPath("$[0].closingBalance").value("10.00"))
			.andExpect(jsonPath("$[0].deposits").value("10.00"))
			.andExpect(jsonPath("$[0].entryCount").value(1));
	}

	@Test
	void returnsAnEmptyListBeforeTheFirstStatement() throws Exception {
		String token = registerAndLogin();
		String account = createAccount(token, "USD");

		mockMvc.perform(get("/api/accounts/{accountNumber}/statements", account)
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void cannotReadAnotherUsersStatements() throws Exception {
		String ownerToken = registerAndLogin();
		String otherToken = registerAndLogin();
		String account = createAccount(ownerToken, "USD");

		mockMvc.perform(get("/api/accounts/{accountNumber}/statements", account)
				.header("Authorization", "Bearer " + otherToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Account not found"));
	}

	@Test
	void unknownAccountReturns404() throws Exception {
		String token = registerAndLogin();

		mockMvc.perform(get("/api/accounts/{accountNumber}/statements", "0000000000")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Account not found"));
	}

	@Test
	void requiresAuthentication() throws Exception {
		String token = registerAndLogin();
		String account = createAccount(token, "USD");

		mockMvc.perform(get("/api/accounts/{accountNumber}/statements", account))
			.andExpect(status().isUnauthorized());
	}

}
