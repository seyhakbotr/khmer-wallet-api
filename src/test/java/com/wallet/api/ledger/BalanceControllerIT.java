package com.wallet.api.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import com.wallet.api.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BalanceControllerIT extends AbstractIntegrationTest {

	@Test
	void freshAccountBalanceIsZero() throws Exception {
		String token = registerAndLogin();
		String accountNumber = createAccount(token, "USD");

		balance(token, accountNumber)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accountNumber").value(accountNumber))
			.andExpect(jsonPath("$.currency").value("USD"))
			.andExpect(jsonPath("$.balance").value("0.00"));
	}

	@Test
	void reflectsDepositsAndWithdrawals() throws Exception {
		String token = registerAndLogin();
		String accountNumber = createAccount(token, "USD");
		deposit(token, accountNumber, "10.50").andExpect(status().isCreated());
		withdraw(token, accountNumber, "4.00").andExpect(status().isCreated());

		balance(token, accountNumber)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.balance").value("6.50"));
	}

	@Test
	void khrBalanceUsesTwoMinorDecimals() throws Exception {
		String token = registerAndLogin();
		String accountNumber = createAccount(token, "KHR");
		deposit(token, accountNumber, "100.50").andExpect(status().isCreated());

		balance(token, accountNumber)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.currency").value("KHR"))
			.andExpect(jsonPath("$.balance").value("100.50"));
	}

	@Test
	void anotherUserCannotReadTheBalance() throws Exception {
		String ownerToken = registerAndLogin();
		String otherToken = registerAndLogin();
		String accountNumber = createAccount(ownerToken, "USD");
		deposit(ownerToken, accountNumber, "10.00").andExpect(status().isCreated());

		balance(otherToken, accountNumber)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Account not found"));

		balance(ownerToken, accountNumber)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.balance").value("10.00"));
	}

	@Test
	void unknownAccountReturns404() throws Exception {
		String token = registerAndLogin();

		balance(token, "0000000000")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Account not found"));
	}

	@Test
	void requiresAuthentication() throws Exception {
		String token = registerAndLogin();
		String accountNumber = createAccount(token, "USD");

		mockMvc.perform(get("/api/accounts/{accountNumber}/balance", accountNumber))
			.andExpect(status().isUnauthorized());
	}

	private ResultActions balance(String token, String accountNumber) throws Exception {
		return mockMvc.perform(get("/api/accounts/{accountNumber}/balance", accountNumber)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}
}
