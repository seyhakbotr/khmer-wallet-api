package com.wallet.api.transfer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.wallet.api.account.Account;
import com.wallet.api.common.Currency;
import com.wallet.api.common.Money;
import com.wallet.api.user.User;

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
import jakarta.persistence.UniqueConstraint;

/**
 * A money movement requested by a user. The transaction row is the idempotency record:
 * one {@code (user, idempotency_key)} pair can create it at most once.
 *
 * <p>The two {@code ledger_entries} rows that belong to a transfer point back here, so
 * "debit + credit = 0" is always verifiable for the transaction.</p>
 */
@Entity
@Table(name = "transactions",
		uniqueConstraints = @UniqueConstraint(name = "uk_transactions_user_idempotency",
				columnNames = { "user_id", "idempotency_key" }))
public class WalletTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "idempotency_key", nullable = false, length = 100)
	private String idempotencyKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_type", nullable = false, length = 20)
	private TransactionType transactionType;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "source_account_id", nullable = false)
	private Account sourceAccount;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "destination_account_id", nullable = false)
	private Account destinationAccount;

	@Column(name = "amount_minor", nullable = false)
	private long amountMinor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 3)
	private Currency currency;

	@Column(name = "source_balance_after_minor", nullable = false)
	private long sourceBalanceAfterMinor;

	@Column(name = "destination_balance_after_minor", nullable = false)
	private long destinationBalanceAfterMinor;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected WalletTransaction() {
		// for JPA
	}

	private WalletTransaction(User user, String idempotencyKey, TransactionType transactionType, Account sourceAccount,
			Account destinationAccount, Money amount, long sourceBalanceAfterMinor,
			long destinationBalanceAfterMinor) {
		if (sourceAccount.getId().equals(destinationAccount.getId())) {
			throw new IllegalArgumentException("source and destination must differ");
		}
		if (amount.amountMinor() <= 0) {
			throw new IllegalArgumentException("amount must be positive");
		}
		this.user = user;
		this.idempotencyKey = idempotencyKey;
		this.transactionType = transactionType;
		this.sourceAccount = sourceAccount;
		this.destinationAccount = destinationAccount;
		this.amountMinor = amount.amountMinor();
		this.currency = amount.currency();
		this.sourceBalanceAfterMinor = sourceBalanceAfterMinor;
		this.destinationBalanceAfterMinor = destinationBalanceAfterMinor;
		this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

	public static WalletTransaction transfer(User user, String idempotencyKey, Account sourceAccount,
			Account destinationAccount, Money amount, long sourceBalanceAfterMinor,
			long destinationBalanceAfterMinor) {
		return new WalletTransaction(user, idempotencyKey, TransactionType.TRANSFER, sourceAccount,
				destinationAccount, amount, sourceBalanceAfterMinor, destinationBalanceAfterMinor);
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public TransactionType getTransactionType() {
		return transactionType;
	}

	public Account getSourceAccount() {
		return sourceAccount;
	}

	public Account getDestinationAccount() {
		return destinationAccount;
	}

	public long getAmountMinor() {
		return amountMinor;
	}

	public Currency getCurrency() {
		return currency;
	}

	public long getSourceBalanceAfterMinor() {
		return sourceBalanceAfterMinor;
	}

	public long getDestinationBalanceAfterMinor() {
		return destinationBalanceAfterMinor;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
