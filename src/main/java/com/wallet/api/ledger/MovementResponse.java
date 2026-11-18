package com.wallet.api.ledger;

import java.time.Instant;

import com.wallet.api.common.Currency;
import com.wallet.api.common.Money;

public record MovementResponse(Long entryId, String accountNumber, Currency currency, String amount, String balance,
		Instant createdAt) {

	public static MovementResponse from(LedgerEntry entry, Money amount, long balanceMinor) {
		return new MovementResponse(
				entry.getId(),
				entry.getAccount().getAccountNumber(),
				entry.getAccount().getCurrency(),
				Money.format(amount),
				Money.format(balanceMinor),
				entry.getCreatedAt());
	}
}
