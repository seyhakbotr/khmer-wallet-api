package com.wallet.api.transfer;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.wallet.api.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The tests that justify the pessimistic lock: two requests read the same balance at the
 * same time, and only one of them is allowed to spend it.
 */
class TransferConcurrencyIT extends AbstractIntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void onlyOneOfTwoConcurrentFullBalanceWithdrawalsSucceeds() throws Exception {
		// Without the FOR UPDATE lock both threads can read 10.00 and both succeed, leaving -10.00.
		String token = registerAndLogin();
		String account = createAccount(token, "USD");
		deposit(token, account, "10.00").andExpect(status().isCreated());

		List<Integer> statuses = runTogether(
				() -> withdraw(token, account, "10.00").andReturn().getResponse().getStatus(),
				() -> withdraw(token, account, "10.00").andReturn().getResponse().getStatus());

		assertThat(statuses).containsExactlyInAnyOrder(201, 422);
		assertThat(balanceOf(account)).isZero();
		assertThat(entryCount(account)).isEqualTo(2);
	}

	@Test
	void onlyOneOfTwoConcurrentFullBalanceTransfersSucceeds() throws Exception {
		String token = registerAndLogin();
		String source = createAccount(token, "USD");
		String firstDestination = createAccount(registerAndLogin(), "USD");
		String secondDestination = createAccount(registerAndLogin(), "USD");
		deposit(token, source, "10.00").andExpect(status().isCreated());

		List<Integer> statuses = runTogether(
				() -> transfer(token, "concurrent-a-" + source, source, firstDestination, "10.00")
					.andReturn().getResponse().getStatus(),
				() -> transfer(token, "concurrent-b-" + source, source, secondDestination, "10.00")
					.andReturn().getResponse().getStatus());

		assertThat(statuses).containsExactlyInAnyOrder(201, 422);
		assertThat(balanceOf(source)).isZero();
		assertThat(balanceOf(firstDestination) + balanceOf(secondDestination)).isEqualTo(1_000);
		assertThat(transactionCountForSource(source)).isEqualTo(1);
		assertThat(entryCount(source)).isEqualTo(2);
	}

	private List<Integer> runTogether(Callable<Integer> first, Callable<Integer> second) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		try {
			Future<Integer> firstResult = pool.submit(gated(first, ready, start));
			Future<Integer> secondResult = pool.submit(gated(second, ready, start));
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			return List.of(firstResult.get(10, TimeUnit.SECONDS), secondResult.get(10, TimeUnit.SECONDS));
		}
		finally {
			pool.shutdownNow();
		}
	}

	private Callable<Integer> gated(Callable<Integer> request, CountDownLatch ready, CountDownLatch start) {
		return () -> {
			ready.countDown();
			start.await();
			return request.call();
		};
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

	private long transactionCountForSource(String accountNumber) {
		return jdbc.queryForObject("select count(*) from transactions where source_account_id = ?", Long.class,
				accountId(accountNumber));
	}

}
