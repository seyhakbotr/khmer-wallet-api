package com.wallet.api.transfer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import com.wallet.api.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransferControllerIT extends AbstractIntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void transfersMoneyAndWritesBothLedgerSides() throws Exception {
		String senderToken = registerAndLogin();
		String recipientToken = registerAndLogin();
		String source = createAccount(senderToken, "USD");
		String destination = createAccount(recipientToken, "USD");
		deposit(senderToken, source, "50.00").andExpect(status().isCreated());

		MvcResult result = transfer(senderToken, "transfer-1", source, destination, "12.34")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.transactionId").isNumber())
			.andExpect(jsonPath("$.idempotencyKey").value("transfer-1"))
			.andExpect(jsonPath("$.sourceAccountNumber").value(source))
			.andExpect(jsonPath("$.destinationAccountNumber").value(destination))
			.andExpect(jsonPath("$.currency").value("USD"))
			.andExpect(jsonPath("$.amount").value("12.34"))
			.andExpect(jsonPath("$.sourceBalance").value("37.66"))
			.andExpect(jsonPath("$.destinationBalance").value("12.34"))
			.andExpect(jsonPath("$.createdAt").exists())
			.andReturn();

		long transactionId = ((Number) com.jayway.jsonpath.JsonPath
			.read(result.getResponse().getContentAsString(), "$.transactionId")).longValue();

		assertThat(balanceOf(source)).isEqualTo(3_766);
		assertThat(balanceOf(destination)).isEqualTo(1_234);
		assertThat(balanceOf(source) + balanceOf(destination)).isEqualTo(5_000);

		List<Map<String, Object>> entries = jdbc.queryForList("""
				select account_id, amount_minor, entry_type
				from ledger_entries where transaction_id = ? order by amount_minor
				""", transactionId);
		assertThat(entries).hasSize(2);
		assertThat(entries.get(0)).containsEntry("amount_minor", -1_234L)
			.containsEntry("entry_type", "TRANSFER_OUT")
			.containsEntry("account_id", accountId(source));
		assertThat(entries.get(1)).containsEntry("amount_minor", 1_234L)
			.containsEntry("entry_type", "TRANSFER_IN")
			.containsEntry("account_id", accountId(destination));
	}

	@Test
	void insufficientFundsReturns422AndWritesNothing() throws Exception {
		String token = registerAndLogin();
		String source = createAccount(token, "USD");
		String destination = createAccount(registerAndLogin(), "USD");
		deposit(token, source, "5.00").andExpect(status().isCreated());

		transfer(token, uniqueKey(), source, destination, "5.01")
			.andExpect(status().is(422))
			.andExpect(jsonPath("$.message").value("Insufficient funds"));

		assertThat(balanceOf(source)).isEqualTo(500);
		assertThat(entryCount(source)).isEqualTo(1);
		assertThat(entryCount(destination)).isZero();
		assertThat(transactionCountForSource(source)).isZero();
	}

	@Test
	void crossCurrencyTransferReturns422AndWritesNothing() throws Exception {
		String token = registerAndLogin();
		String usdAccount = createAccount(token, "USD");
		String khrAccount = createAccount(token, "KHR");
		deposit(token, usdAccount, "10.00").andExpect(status().isCreated());

		transfer(token, uniqueKey(), usdAccount, khrAccount, "5.00")
			.andExpect(status().is(422))
			.andExpect(jsonPath("$.message").value("Cross-currency transfers are not supported"));

		assertThat(entryCount(usdAccount)).isEqualTo(1);
		assertThat(entryCount(khrAccount)).isZero();
		assertThat(transactionCountForSource(usdAccount)).isZero();
	}

	@Test
	void transferringToTheSameAccountReturns422() throws Exception {
		String token = registerAndLogin();
		String account = createAccount(token, "USD");
		deposit(token, account, "10.00").andExpect(status().isCreated());

		transfer(token, uniqueKey(), account, account, "1.00")
			.andExpect(status().is(422))
			.andExpect(jsonPath("$.message").value("Source and destination accounts must differ"));

		assertThat(entryCount(account)).isEqualTo(1);
		assertThat(transactionCountForSource(account)).isZero();
	}

	@Test
	void unknownAccountsReturn404() throws Exception {
		String token = registerAndLogin();
		String account = createAccount(token, "USD");
		deposit(token, account, "10.00").andExpect(status().isCreated());

		transfer(token, uniqueKey(), account, "0000000000", "1.00")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Account not found"));

		transfer(token, uniqueKey(), "0000000000", account, "1.00")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Account not found"));

		assertThat(entryCount(account)).isEqualTo(1);
		assertThat(transactionCountForSource(account)).isZero();
	}

	@Test
	void cannotTransferFromAnotherUsersAccount() throws Exception {
		String ownerToken = registerAndLogin();
		String otherToken = registerAndLogin();
		String ownerAccount = createAccount(ownerToken, "USD");
		String otherAccount = createAccount(otherToken, "USD");
		deposit(ownerToken, ownerAccount, "10.00").andExpect(status().isCreated());

		transfer(otherToken, uniqueKey(), ownerAccount, otherAccount, "1.00")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Account not found"));

		assertThat(entryCount(ownerAccount)).isEqualTo(1);
		assertThat(entryCount(otherAccount)).isZero();
		assertThat(transactionCountForSource(ownerAccount)).isZero();
	}

	@Test
	void invalidAmountsReturn400AndWriteNothing() throws Exception {
		String token = registerAndLogin();
		String source = createAccount(token, "USD");
		String destination = createAccount(registerAndLogin(), "USD");
		deposit(token, source, "10.00").andExpect(status().isCreated());

		List<String> invalidAmounts = List.of(
				"10.555", "0", "0.00", "-5.00", "+5.00", "abc", "1e5",
				"99999999999999999999.99", "1000000.01");

		for (String amount : invalidAmounts) {
			transfer(token, uniqueKey(), source, destination, amount)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.amount").exists());
		}

		assertThat(entryCount(source)).isEqualTo(1);
		assertThat(entryCount(destination)).isZero();
		assertThat(transactionCountForSource(source)).isZero();
	}

	@Test
	void missingOrInvalidIdempotencyKeyReturns400() throws Exception {
		String token = registerAndLogin();
		String source = createAccount(token, "USD");
		String destination = createAccount(registerAndLogin(), "USD");
		deposit(token, source, "10.00").andExpect(status().isCreated());

		mockMvc.perform(post("/api/transfers")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(transferBody(source, destination, "1.00")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors['Idempotency-Key']").exists());

		for (String key : List.of("   ", "x".repeat(101), "has space")) {
			transfer(token, key, source, destination, "1.00")
				.andExpect(status().isBadRequest());
		}

		assertThat(entryCount(source)).isEqualTo(1);
		assertThat(transactionCountForSource(source)).isZero();
	}

	@Test
	void requiresAuthentication() throws Exception {
		String token = registerAndLogin();
		String source = createAccount(token, "USD");
		String destination = createAccount(registerAndLogin(), "USD");
		deposit(token, source, "10.00").andExpect(status().isCreated());

		mockMvc.perform(post("/api/transfers")
				.header("Idempotency-Key", uniqueKey())
				.contentType(MediaType.APPLICATION_JSON)
				.content(transferBody(source, destination, "1.00")))
			.andExpect(status().isUnauthorized());

		assertThat(entryCount(source)).isEqualTo(1);
	}

	@Test
	void sameIdempotencyKeyReturnsTheSameResultAndMovesMoneyOnce() throws Exception {
		String token = registerAndLogin();
		String source = createAccount(token, "USD");
		String destination = createAccount(registerAndLogin(), "USD");
		deposit(token, source, "20.00").andExpect(status().isCreated());
		String key = uniqueKey();

		MvcResult first = transfer(token, key, source, destination, "7.50")
			.andExpect(status().isCreated())
			.andReturn();
		String firstBody = first.getResponse().getContentAsString();

		MvcResult replay = transfer(token, key, source, destination, "7.50")
			.andExpect(status().isCreated())
			.andReturn();

		assertThat(replay.getResponse().getContentAsString()).isEqualTo(firstBody);
		assertThat(balanceOf(source)).isEqualTo(1_250);
		assertThat(balanceOf(destination)).isEqualTo(750);
		assertThat(transactionCountForKey(key)).isEqualTo(1);
		assertThat(entryCount(source)).isEqualTo(2);
		assertThat(entryCount(destination)).isEqualTo(1);
	}

	@Test
	void replayAcceptsTheSameAmountWithDifferentFormatting() throws Exception {
		String token = registerAndLogin();
		String source = createAccount(token, "USD");
		String destination = createAccount(registerAndLogin(), "USD");
		deposit(token, source, "20.00").andExpect(status().isCreated());
		String key = uniqueKey();

		transfer(token, key, source, destination, "7.50").andExpect(status().isCreated());
		transfer(token, key, source, destination, "7.5").andExpect(status().isCreated());

		assertThat(balanceOf(source)).isEqualTo(1_250);
		assertThat(transactionCountForKey(key)).isEqualTo(1);
	}

	@Test
	void reusingAKeyForADifferentRequestReturns409AndWritesNothing() throws Exception {
		String token = registerAndLogin();
		String source = createAccount(token, "USD");
		String destination = createAccount(registerAndLogin(), "USD");
		String otherDestination = createAccount(registerAndLogin(), "USD");
		deposit(token, source, "20.00").andExpect(status().isCreated());
		String key = uniqueKey();
		transfer(token, key, source, destination, "7.50").andExpect(status().isCreated());

		transfer(token, key, source, destination, "7.51")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("Idempotency-Key was already used with a different request"));

		transfer(token, key, source, otherDestination, "7.50")
			.andExpect(status().isConflict());

		assertThat(balanceOf(source)).isEqualTo(1_250);
		assertThat(entryCount(destination)).isEqualTo(1);
		assertThat(entryCount(otherDestination)).isZero();
		assertThat(transactionCountForKey(key)).isEqualTo(1);
	}

	@Test
	void differentUsersMayReuseTheSameKey() throws Exception {
		String firstToken = registerAndLogin();
		String firstSource = createAccount(firstToken, "USD");
		String firstDestination = createAccount(registerAndLogin(), "USD");
		deposit(firstToken, firstSource, "10.00").andExpect(status().isCreated());

		String secondToken = registerAndLogin();
		String secondSource = createAccount(secondToken, "USD");
		String secondDestination = createAccount(registerAndLogin(), "USD");
		deposit(secondToken, secondSource, "10.00").andExpect(status().isCreated());
		String sharedKey = uniqueKey();

		transfer(firstToken, sharedKey, firstSource, firstDestination, "1.00")
			.andExpect(status().isCreated());
		transfer(secondToken, sharedKey, secondSource, secondDestination, "1.00")
			.andExpect(status().isCreated());

		assertThat(transactionCountForKey(sharedKey)).isEqualTo(2);
	}

	private String uniqueKey() {
		return "key-" + UUID.randomUUID();
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

	private long transactionCountForKey(String key) {
		return jdbc.queryForObject("select count(*) from transactions where idempotency_key = ?", Long.class, key);
	}

}
