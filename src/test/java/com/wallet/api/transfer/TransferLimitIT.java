package com.wallet.api.transfer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.wallet.api.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.transfers.daily-limits.USD=10000")
class TransferLimitIT extends AbstractIntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void dailyLimitIsEnforcedAcrossTransfers() throws Exception {
		String token = registerAndLogin();
		String source = createAccount(token, "USD");
		String destination = createAccount(registerAndLogin(), "USD");
		deposit(token, source, "200.00").andExpect(status().isCreated());

		transfer(token, "limit-a", source, destination, "60.00").andExpect(status().isCreated());
		transfer(token, "limit-b", source, destination, "40.00").andExpect(status().isCreated());

		transfer(token, "limit-c", source, destination, "0.01")
			.andExpect(status().is(422))
			.andExpect(jsonPath("$.message").value("Daily transfer limit exceeded"));

		assertThat(balanceOf(source)).isEqualTo(10_000);
		assertThat(balanceOf(destination)).isEqualTo(10_000);
		assertThat(entryCount(source)).isEqualTo(3);
	}

	@Test
	void limitAppliesPerAccountNotPerUser() throws Exception {
		String firstToken = registerAndLogin();
		String firstSource = createAccount(firstToken, "USD");
		String firstDestination = createAccount(registerAndLogin(), "USD");
		deposit(firstToken, firstSource, "100.00").andExpect(status().isCreated());
		transfer(firstToken, "limit-first", firstSource, firstDestination, "100.00").andExpect(status().isCreated());

		String secondToken = registerAndLogin();
		String secondSource = createAccount(secondToken, "USD");
		String secondDestination = createAccount(registerAndLogin(), "USD");
		deposit(secondToken, secondSource, "100.00").andExpect(status().isCreated());

		transfer(secondToken, "limit-second", secondSource, secondDestination, "100.00")
			.andExpect(status().isCreated());
	}

	@Test
	void idempotentReplayDoesNotConsumeTheLimitTwice() throws Exception {
		String token = registerAndLogin();
		String source = createAccount(token, "USD");
		String destination = createAccount(registerAndLogin(), "USD");
		deposit(token, source, "200.00").andExpect(status().isCreated());

		transfer(token, "limit-replay", source, destination, "100.00").andExpect(status().isCreated());
		transfer(token, "limit-replay", source, destination, "100.00").andExpect(status().isCreated());

		transfer(token, "limit-after-replay", source, destination, "0.01")
			.andExpect(status().is(422))
			.andExpect(jsonPath("$.message").value("Daily transfer limit exceeded"));

		assertThat(balanceOf(source)).isEqualTo(10_000);
	}

	@Test
	void transfersFromYesterdayDoNotCountTowardsToday() throws Exception {
		String token = registerAndLogin();
		String source = createAccount(token, "USD");
		String destination = createAccount(registerAndLogin(), "USD");
		deposit(token, source, "200.00").andExpect(status().isCreated());
		transfer(token, "limit-yesterday", source, destination, "100.00").andExpect(status().isCreated());

		jdbc.update("""
				update ledger_entries set created_at = now() - interval '1 day'
				where account_id = ? and entry_type = 'TRANSFER_OUT'
				""", accountId(source));

		transfer(token, "limit-today", source, destination, "100.00").andExpect(status().isCreated());

		assertThat(balanceOf(source)).isZero();
		assertThat(balanceOf(destination)).isEqualTo(20_000);
	}

	private long accountId(String accountNumber) {
		return jdbc.queryForObject("select id from accounts where account_number = ?", Long.class, accountNumber);
	}

	private long balanceOf(String accountNumber) {
		return jdbc.queryForObject("select coalesce(sum(amount_minor), 0) from ledger_entries where account_id = ?",
				Long.class, accountId(accountNumber));
	}

	private long entryCount(String accountNumber) {
		return jdbc.queryForObject("select count(*) from ledger_entries where account_id = ?", Long.class,
				accountId(accountNumber));
	}

}
