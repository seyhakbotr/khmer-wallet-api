package com.wallet.api.audit;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.wallet.api.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditLogIT extends AbstractIntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void everyMoneyMovementAppendsAnAuditRow() throws Exception {
		String token = registerAndLogin();
		String account = createAccount(token, "USD");
		String destination = createAccount(registerAndLogin(), "USD");

		deposit(token, account, "10.00").andExpect(status().isCreated());
		withdraw(token, account, "4.00").andExpect(status().isCreated());
		transfer(token, "audit-" + account, account, destination, "3.00").andExpect(status().isCreated());

		long userId = jdbc.queryForObject("select user_id from accounts where account_number = ?", Long.class, account);

		List<String> actions = jdbc.queryForList("""
				select action from audit_log where actor_user_id = ? order by id
				""", String.class, userId);
		assertThat(actions).containsExactly("DEPOSIT", "WITHDRAWAL", "TRANSFER");

		List<String> details = jdbc.queryForList("""
				select detail from audit_log where actor_user_id = ? order by id
				""", String.class, userId);
		assertThat(details.get(0)).isEqualTo("10.00 USD into " + account);
		assertThat(details.get(1)).isEqualTo("4.00 USD from " + account);
		assertThat(details.get(2)).isEqualTo("3.00 USD from " + account + " to " + destination);

		assertThat(jdbc.queryForObject("""
				select count(*) from audit_log a
				join ledger_entries e on a.entity_type = 'LEDGER_ENTRY' and a.entity_id = e.id
				where a.actor_user_id = ?
				""", Long.class, userId)).isEqualTo(2);
		assertThat(jdbc.queryForObject("""
				select count(*) from audit_log a
				join transactions t on a.entity_type = 'TRANSACTION' and a.entity_id = t.id
				where a.actor_user_id = ?
				""", Long.class, userId)).isEqualTo(1);
	}

	@Test
	void replayingAnIdempotentTransferDoesNotDuplicateItsAuditRow() throws Exception {
		String token = registerAndLogin();
		String account = createAccount(token, "USD");
		String destination = createAccount(registerAndLogin(), "USD");
		deposit(token, account, "10.00").andExpect(status().isCreated());
		String key = "audit-replay-" + account;

		transfer(token, key, account, destination, "2.00").andExpect(status().isCreated());
		transfer(token, key, account, destination, "2.00").andExpect(status().isCreated());

		assertThat(jdbc.queryForObject("select count(*) from audit_log where action = 'TRANSFER' and detail like ?",
				Long.class, "%to " + destination)).isEqualTo(1);
	}

	@Test
	void auditRowsCannotBeUpdatedOrDeleted() throws Exception {
		long auditId = latestAuditId();
		String originalDetail = auditDetail(auditId);

		assertThatThrownBy(() -> jdbc.update("update audit_log set detail = 'tampered' where id = ?", auditId))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("append-only");

		assertThatThrownBy(() -> jdbc.update("delete from audit_log where id = ?", auditId))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("append-only");

		assertThat(auditDetail(auditId)).isEqualTo(originalDetail);
	}

	@Test
	void auditLogCannotBeTruncated() {
		assertThatThrownBy(() -> jdbc.execute("truncate table audit_log"))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("append-only");
	}

	private long latestAuditId() throws Exception {
		String token = registerAndLogin();
		String account = createAccount(token, "USD");
		deposit(token, account, "1.00").andExpect(status().isCreated());
		return jdbc.queryForObject("select max(id) from audit_log", Long.class);
	}

	private String auditDetail(long auditId) {
		return jdbc.queryForObject("select detail from audit_log where id = ?", String.class, auditId);
	}

}
