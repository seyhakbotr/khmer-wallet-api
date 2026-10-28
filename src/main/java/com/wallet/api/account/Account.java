package com.wallet.api.account;

import java.time.Instant;

import com.wallet.api.common.Currency;
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
 * A wallet account owned by a user, in exactly one currency.
 *
 * <p>Note: there is intentionally no mutable {@code balance} column. Balances will be
 * derived from the ledger in a later phase, so a balance can never drift from history.</p>
 */
@Entity
@Table(name = "accounts",
		uniqueConstraints = @UniqueConstraint(name = "uk_accounts_user_currency",
				columnNames = { "user_id", "currency" }))
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "account_number", nullable = false, unique = true, length = 20)
	private String accountNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 3)
	private Currency currency;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Account() {
		// for JPA
	}

	public Account(User user, String accountNumber, Currency currency) {
		this.user = user;
		this.accountNumber = accountNumber;
		this.currency = currency;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public Currency getCurrency() {
		return currency;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
