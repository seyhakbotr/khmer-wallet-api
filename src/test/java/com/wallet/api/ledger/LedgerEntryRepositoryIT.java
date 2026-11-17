package com.wallet.api.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.wallet.api.AbstractIntegrationTest;
import com.wallet.api.account.Account;
import com.wallet.api.account.AccountRepository;
import com.wallet.api.common.Currency;
import com.wallet.api.common.Money;
import com.wallet.api.user.User;
import com.wallet.api.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the sum that every balance in the API will be derived from.
 *
 * <p>These run against the shared Testcontainers Postgres, and rows are never rolled back
 * between tests — so every test creates its own user and account.</p>
 */
class LedgerEntryRepositoryIT extends AbstractIntegrationTest {

	@Autowired
	private LedgerEntryRepository entries;

	@Autowired
	private AccountRepository accounts;

	@Autowired
	private UserRepository users;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void emptyAccountSumsToZero() {
		Account account = newUsdAccount();

		assertThat(entries.sumAmountMinorByAccountId(account.getId())).isZero();
	}

	@Test
	void depositAndWithdrawalAreSummed() {
		Account account = newUsdAccount();

		entries.save(LedgerEntry.deposit(account, Money.of(1_050, Currency.USD)));
		entries.save(LedgerEntry.withdrawal(account, Money.of(400, Currency.USD)));

		assertThat(entries.sumAmountMinorByAccountId(account.getId())).isEqualTo(650);
	}

	@Test
	void databaseRejectsZeroAmount() {
		Account account = newUsdAccount();

		assertThatThrownBy(() -> jdbc.update("""
				insert into ledger_entries (account_id, amount_minor, entry_type)
				values (?, ?, ?)
				""", account.getId(), 0, "DEPOSIT"))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("ck_ledger_entries_amount_non_zero");
	}

	@Test
	void sumsAreScopedToTheAccount() {
		Account funded = newUsdAccount();
		Account untouched = newUsdAccount();

		entries.save(LedgerEntry.deposit(funded, Money.of(1_050, Currency.USD)));

		assertThat(entries.sumAmountMinorByAccountId(funded.getId())).isEqualTo(1_050);
		assertThat(entries.sumAmountMinorByAccountId(untouched.getId())).isZero();
	}

	private Account newUsdAccount() {
		String phone = uniquePhone();
		User user = users.save(new User("Ledger Test", phone, "password-hash"));
		return accounts.save(new Account(user, "9" + phone.substring(1), Currency.USD));
	}

}
