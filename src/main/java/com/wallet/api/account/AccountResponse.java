package com.wallet.api.account;

import java.time.Instant;

import com.wallet.api.common.Currency;

public record AccountResponse(Long id, String accountNumber, Currency currency, Instant createdAt) {

	public static AccountResponse from(Account account) {
		return new AccountResponse(account.getId(), account.getAccountNumber(), account.getCurrency(),
				account.getCreatedAt());
	}
}
