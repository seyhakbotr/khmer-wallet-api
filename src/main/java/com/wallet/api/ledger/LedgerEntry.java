package com.wallet.api.ledger;

import java.time.Instant;

import com.wallet.api.account.Account;
import com.wallet.api.common.Money;
import com.wallet.api.transfer.WalletTransaction;

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
 * <p>Sign and {@link EntryType} must agree: deposits and transfers in are positive,
 * withdrawals and transfers out are negative. Transfer entries also point at the
 * transaction that produced them, which is how the two sides are kept together.</p>
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transaction_id")
	private WalletTransaction transaction;

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
		this(account, amountMinor, entryType, null);
	}

	public LedgerEntry(Account account, long amountMinor, EntryType entryType, WalletTransaction transaction) {
		if (amountMinor == 0) {
			throw new IllegalArgumentException("amount must not be zero");
		}
		if (!entryType.matches(amountMinor)) {
			throw new IllegalArgumentException(entryType + " does not match amount " + amountMinor);
		}
		if (entryType.isTransfer() != (transaction != null)) {
			throw new IllegalArgumentException(entryType + " requires a matching transaction");
		}
		this.account = account;
		this.transaction = transaction;
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

	public static LedgerEntry transferIn(Account account, WalletTransaction transaction, Money amount) {
		return new LedgerEntry(account, amount.amountMinor(), EntryType.TRANSFER_IN, transaction);
	}

	public static LedgerEntry transferOut(Account account, WalletTransaction transaction, Money amount) {
		return new LedgerEntry(account, Math.negateExact(amount.amountMinor()), EntryType.TRANSFER_OUT, transaction);
	}

	public Long getId() {
		return id;
	}

	public Account getAccount() {
		return account;
	}

	public WalletTransaction getTransaction() {
		return transaction;
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
