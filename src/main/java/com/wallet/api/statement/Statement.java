package com.wallet.api.statement;

import java.time.Instant;
import java.time.LocalDate;

import com.wallet.api.account.Account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A finalized end-of-day snapshot for one account. Statements are generated once per
 * account and date; the unique constraint makes re-running the job harmless.
 */
@Entity
@Table(name = "statements",
		uniqueConstraints = @UniqueConstraint(name = "uk_statements_account_date",
				columnNames = { "account_id", "statement_date" }))
public class Statement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	@Column(name = "statement_date", nullable = false, updatable = false)
	private LocalDate statementDate;

	@Column(name = "opening_balance_minor", nullable = false)
	private long openingBalanceMinor;

	@Column(name = "closing_balance_minor", nullable = false)
	private long closingBalanceMinor;

	@Column(name = "deposits_minor", nullable = false)
	private long depositsMinor;

	@Column(name = "withdrawals_minor", nullable = false)
	private long withdrawalsMinor;

	@Column(name = "transfers_in_minor", nullable = false)
	private long transfersInMinor;

	@Column(name = "transfers_out_minor", nullable = false)
	private long transfersOutMinor;

	@Column(name = "entry_count", nullable = false)
	private long entryCount;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Statement() {
		// for JPA
	}

	private Statement(Account account, LocalDate statementDate, long openingBalanceMinor, long closingBalanceMinor,
			long depositsMinor, long withdrawalsMinor, long transfersInMinor, long transfersOutMinor, long entryCount) {
		this.account = account;
		this.statementDate = statementDate;
		this.openingBalanceMinor = openingBalanceMinor;
		this.closingBalanceMinor = closingBalanceMinor;
		this.depositsMinor = depositsMinor;
		this.withdrawalsMinor = withdrawalsMinor;
		this.transfersInMinor = transfersInMinor;
		this.transfersOutMinor = transfersOutMinor;
		this.entryCount = entryCount;
		this.createdAt = Instant.now();
	}

	public static Statement of(Account account, LocalDate statementDate, long openingBalanceMinor,
			long closingBalanceMinor, long depositsMinor, long withdrawalsMinor, long transfersInMinor,
			long transfersOutMinor, long entryCount) {
		return new Statement(account, statementDate, openingBalanceMinor, closingBalanceMinor, depositsMinor,
				withdrawalsMinor, transfersInMinor, transfersOutMinor, entryCount);
	}

	public Long getId() {
		return id;
	}

	public Account getAccount() {
		return account;
	}

	public LocalDate getStatementDate() {
		return statementDate;
	}

	public long getOpeningBalanceMinor() {
		return openingBalanceMinor;
	}

	public long getClosingBalanceMinor() {
		return closingBalanceMinor;
	}

	public long getDepositsMinor() {
		return depositsMinor;
	}

	public long getWithdrawalsMinor() {
		return withdrawalsMinor;
	}

	public long getTransfersInMinor() {
		return transfersInMinor;
	}

	public long getTransfersOutMinor() {
		return transfersOutMinor;
	}

	public long getEntryCount() {
		return entryCount;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
