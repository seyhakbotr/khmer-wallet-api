package com.wallet.api.ledger;

import java.time.Instant;

import com.wallet.api.account.Account;
import com.wallet.api.common.Money;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One movement of money on one account. Balances are {@code SUM(amountMinor)} over these
 * rows, so rows are only ever inserted — never updated or deleted.
 *
 * <p>Sign and {@link EntryType} must agree: deposits are positive, withdrawals negative.</p>
 */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	@Column(name = "amount_minor", nullable = false)
	private long amountMinor;

	@Enumerated(EnumType.STRING)
	@Column(name = "entry_type", nullable = false, length = 20)
	private EntryType entryType;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected LedgerEntry() {
		// for JPA
	}

	public LedgerEntry(Account account, long amountMinor, EntryType entryType) {
		if (amountMinor == 0) {
			throw new IllegalArgumentException("amount must not be zero");
		}
		if (!entryType.matches(amountMinor)) {
			throw new IllegalArgumentException(entryType + " does not match amount " + amountMinor);
		}
		this.account = account;
		this.amountMinor = amountMinor;
		this.entryType = entryType;
		this.createdAt = Instant.now();
	}

	public static LedgerEntry deposit(Account account, Money amount) {
		return new LedgerEntry(account, amount.amountMinor(), EntryType.DEPOSIT);
	}

	public static LedgerEntry withdrawal(Account account, Money amount) {
		return new LedgerEntry(account, Math.negateExact(amount.amountMinor()), EntryType.WITHDRAWAL);
	}

	public Long getId() {
		return id;
	}

	public Account getAccount() {
		return account;
	}

	public long getAmountMinor() {
		return amountMinor;
	}

	public EntryType getEntryType() {
		return entryType;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
