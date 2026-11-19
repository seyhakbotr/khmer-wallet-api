package com.wallet.api.statement;

import java.time.Clock;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.wallet.api.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatementServiceIT extends AbstractIntegrationTest {

	@Autowired
	private StatementService statementService;

	@Autowired
	private StatementJob statementJob;

	@Autowired
	private StatementRepository statements;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private Clock clock;

	@Test
	void statementSummarizesEveryMovementKind() throws Exception {
		String token = registerAndLogin();
		String account = createAccount(token, "USD");
		String destination = createAccount(registerAndLogin(), "USD");
		deposit(token, account, "100.00").andExpect(status().isCreated());
		withdraw(token, account, "30.00").andExpect(status().isCreated());
		transfer(token, "statement-transfer", account, destination, "20.00").andExpect(status().isCreated());

		LocalDate today = LocalDate.now(clock);
		statementService.generateDailyStatements(today);

		Statement statement = statements.findByAccountIdAndStatementDate(accountId(account), today).orElseThrow();
		assertThat(statement.getOpeningBalanceMinor()).isZero();
		assertThat(statement.getClosingBalanceMinor()).isEqualTo(5_000);
		assertThat(statement.getDepositsMinor()).isEqualTo(10_000);
		assertThat(statement.getWithdrawalsMinor()).isEqualTo(3_000);
		assertThat(statement.getTransfersOutMinor()).isEqualTo(2_000);
		assertThat(statement.getTransfersInMinor()).isZero();
		assertThat(statement.getEntryCount()).isEqualTo(3);

		Statement destinationStatement = statements
			.findByAccountIdAndStatementDate(accountId(destination), today)
			.orElseThrow();
		assertThat(destinationStatement.getOpeningBalanceMinor()).isZero();
		assertThat(destinationStatement.getClosingBalanceMinor()).isEqualTo(2_000);
		assertThat(destinationStatement.getTransfersInMinor()).isEqualTo(2_000);
		assertThat(destinationStatement.getEntryCount()).isEqualTo(1);
	}

	@Test
	void scheduledJobGeneratesYesterdaysStatement() throws Exception {
		String token = registerAndLogin();
		String account = createAccount(token, "USD");
		deposit(token, account, "10.00").andExpect(status().isCreated());

		jdbc.update("update accounts set created_at = now() - interval '2 days' where id = ?", accountId(account));
		jdbc.update("update ledger_entries set created_at = now() - interval '1 day' where account_id = ?",
				accountId(account));

		statementJob.generateYesterday();

		LocalDate yesterday = LocalDate.now(clock).minusDays(1);
		Statement statement = statements.findByAccountIdAndStatementDate(accountId(account), yesterday).orElseThrow();
		assertThat(statement.getOpeningBalanceMinor()).isZero();
		assertThat(statement.getClosingBalanceMinor()).isEqualTo(1_000);
		assertThat(statement.getDepositsMinor()).isEqualTo(1_000);
		assertThat(statement.getEntryCount()).isEqualTo(1);
	}

	@Test
	void rerunningGenerationDoesNotDuplicateOrChangeAStatement() throws Exception {
		String token = registerAndLogin();
		String account = createAccount(token, "USD");
		deposit(token, account, "10.00").andExpect(status().isCreated());

		LocalDate today = LocalDate.now(clock);
		statementService.generateDailyStatements(today);
		withdraw(token, account, "4.00").andExpect(status().isCreated());
		statementService.generateDailyStatements(today);

		assertThat(jdbc.queryForObject("select count(*) from statements where account_id = ? and statement_date = ?",
				Long.class, accountId(account), today)).isEqualTo(1);
		Statement statement = statements.findByAccountIdAndStatementDate(accountId(account), today).orElseThrow();
		assertThat(statement.getClosingBalanceMinor()).isEqualTo(1_000);
		assertThat(statement.getWithdrawalsMinor()).isZero();
	}

	@Test
	void nextDayOpeningEqualsPreviousClosing() throws Exception {
		String token = registerAndLogin();
		String account = createAccount(token, "USD");
		deposit(token, account, "10.00").andExpect(status().isCreated());

		LocalDate today = LocalDate.now(clock);
		statementService.generateDailyStatements(today);
		statementService.generateDailyStatements(today.plusDays(1));

		Statement first = statements.findByAccountIdAndStatementDate(accountId(account), today).orElseThrow();
		Statement second = statements.findByAccountIdAndStatementDate(accountId(account), today.plusDays(1))
			.orElseThrow();

		assertThat(second.getOpeningBalanceMinor()).isEqualTo(first.getClosingBalanceMinor());
		assertThat(second.getClosingBalanceMinor()).isEqualTo(1_000);
		assertThat(second.getEntryCount()).isZero();
	}

	@Test
	void accountsCreatedAfterTheStatementDateAreSkipped() throws Exception {
		String token = registerAndLogin();
		String account = createAccount(token, "USD");

		LocalDate yesterday = LocalDate.now(clock).minusDays(1);
		statementService.generateDailyStatements(yesterday);

		assertThat(statements.findByAccountIdAndStatementDate(accountId(account), yesterday)).isEmpty();
	}

	private long accountId(String accountNumber) {
		return jdbc.queryForObject("select id from accounts where account_number = ?", Long.class, accountNumber);
	}

}
