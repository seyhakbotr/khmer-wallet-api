package com.wallet.api.transfer;

import java.time.Instant;

import com.wallet.api.common.Currency;
import com.wallet.api.common.Money;

public record TransferResponse(Long transactionId, String idempotencyKey, String sourceAccountNumber,
		String destinationAccountNumber, Currency currency, String amount, String sourceBalance,
		String destinationBalance, Instant createdAt) {

	public static TransferResponse from(WalletTransaction transaction) {
		return new TransferResponse(
				transaction.getId(),
				transaction.getIdempotencyKey(),
				transaction.getSourceAccount().getAccountNumber(),
				transaction.getDestinationAccount().getAccountNumber(),
				transaction.getCurrency(),
				Money.format(transaction.getAmountMinor()),
				Money.format(transaction.getSourceBalanceAfterMinor()),
				Money.format(transaction.getDestinationBalanceAfterMinor()),
				transaction.getCreatedAt());
	}

}
