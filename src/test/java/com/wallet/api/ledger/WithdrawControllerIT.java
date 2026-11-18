package com.wallet.api.ledger;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import com.wallet.api.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WithdrawControllerIT extends AbstractIntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void withdrawsMoneyAndReturnsNewBalance() throws Exception {
		String token = registerAndLogin();
		String accountNumber = createAccount(token, "USD");
		deposit(token, accountNumber, "10.50").andExpect(status().isCreated());

		withdraw(token, accountNumber, "4.00")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.entryId").isNumber())
			.andExpect(jsonPath("$.accountNumber").value(accountNumber))
			.andExpect(jsonPath("$.currency").value("USD"))
			.andExpect(jsonPath("$.amount").value("4.00"))
			.andExpect(jsonPath("$.balance").value("6.50"))
			.andExpect(jsonPath("$.createdAt").exists());

		assertThat(entryCount(accountNumber)).isEqualTo(2);
		assertThat(entrySum(accountNumber)).isEqualTo(650);
		assertThat(latestAmount(accountNumber)).isEqualTo(-400);
		assertThat(latestType(accountNumber)).isEqualTo("WITHDRAWAL");
	}

	@Test
	void exactBalanceWithdrawalLeavesZero() throws Exception {
		String token = registerAndLogin();
		String accountNumber = createAccount(token, "USD");
		deposit(token, accountNumber, "6.50").andExpect(status().isCreated());

		withdraw(token, accountNumber, "6.50")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.amount").value("6.50"))
			.andExpect(jsonPath("$.balance").value("0.00"));

		assertThat(entryCount(accountNumber)).isEqualTo(2);
		assertThat(entrySum(accountNumber)).isZero();
	}

	@Test
	void overdraftReturns422AndWritesNoEntry() throws Exception {
		String token = registerAndLogin();
		String accountNumber = createAccount(token, "USD");
		deposit(token, accountNumber, "6.50").andExpect(status().isCreated());
		long entriesBefore = entryCount(accountNumber);
		long balanceBefore = entrySum(accountNumber);

		withdraw(token, accountNumber, "100.00")
			.andExpect(status().is(422))
			.andExpect(jsonPath("$.status").value(422))
			.andExpect(jsonPath("$.message").value("Insufficient funds"));

		assertThat(entryCount(accountNumber)).isEqualTo(entriesBefore);
		assertThat(entrySum(accountNumber)).isEqualTo(balanceBefore);
	}

	@Test
	void withdrawingFromAnEmptyAccountReturns422() throws Exception {
		String token = registerAndLogin();
		String accountNumber = createAccount(token, "USD");

		withdraw(token, accountNumber, "1.00")
			.andExpect(status().is(422))
			.andExpect(jsonPath("$.message").value("Insufficient funds"));

		assertThat(entryCount(accountNumber)).isZero();
	}

	@Test
	void cannotWithdrawFromAnotherUsersAccount() throws Exception {
		String ownerToken = registerAndLogin();
		String otherToken = registerAndLogin();
		String accountNumber = createAccount(ownerToken, "USD");
		deposit(ownerToken, accountNumber, "10.00").andExpect(status().isCreated());

		withdraw(otherToken, accountNumber, "4.00")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Account not found"));

		assertThat(entryCount(accountNumber)).isEqualTo(1);
		assertThat(entrySum(accountNumber)).isEqualTo(1_000);
	}

	@Test
	void unknownAccountReturns404() throws Exception {
		String token = registerAndLogin();

		withdraw(token, "0000000000", "10.00")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Account not found"));
	}

	@Test
	void invalidAmountsReturn400AndWriteNoEntry() throws Exception {
		String token = registerAndLogin();
		String accountNumber = createAccount(token, "USD");

		List<String> invalidAmounts = List.of(
				"10.555", "0", "0.00", "-5.00", "+5.00", "abc", "1e5",
				"99999999999999999999.99", "1000000.01");

		for (String amount : invalidAmounts) {
			withdraw(token, accountNumber, amount)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.amount").exists());
		}

		assertThat(entryCount(accountNumber)).isZero();
	}

	@Test
	void requiresAuthentication() throws Exception {
		String token = registerAndLogin();
		String accountNumber = createAccount(token, "USD");

		mockMvc.perform(post("/api/accounts/{accountNumber}/withdrawals", accountNumber)
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

	private long entrySum(String accountNumber) {
		return jdbc.queryForObject("select coalesce(sum(amount_minor), 0) from ledger_entries where account_id = ?",
				Long.class, accountId(accountNumber));
	}

	private long latestAmount(String accountNumber) {
		return jdbc.queryForObject("""
				select amount_minor from ledger_entries
				where account_id = ? order by id desc limit 1
				""", Long.class, accountId(accountNumber));
	}

	private String latestType(String accountNumber) {
		return jdbc.queryForObject("""
				select entry_type from ledger_entries
				where account_id = ? order by id desc limit 1
				""", String.class, accountId(accountNumber));
	}

	private long accountId(String accountNumber) {
		return jdbc.queryForObject("select id from accounts where account_number = ?",
				Long.class, accountNumber);
	}
}
