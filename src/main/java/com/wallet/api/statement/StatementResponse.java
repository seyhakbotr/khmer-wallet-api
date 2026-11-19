package com.wallet.api.statement;

import java.time.Instant;
import java.time.LocalDate;

import com.wallet.api.common.Currency;
import com.wallet.api.common.Money;

public record StatementResponse(String accountNumber, Currency currency, LocalDate statementDate,
		String openingBalance, String closingBalance, String deposits, String withdrawals, String transfersIn,
		String transfersOut, long entryCount, Instant createdAt) {

	public static StatementResponse from(Statement statement) {
		return new StatementResponse(
				statement.getAccount().getAccountNumber(),
				statement.getAccount().getCurrency(),
				statement.getStatementDate(),
				Money.format(statement.getOpeningBalanceMinor()),
				Money.format(statement.getClosingBalanceMinor()),
				Money.format(statement.getDepositsMinor()),
				Money.format(statement.getWithdrawalsMinor()),
				Money.format(statement.getTransfersInMinor()),
				Money.format(statement.getTransfersOutMinor()),
				statement.getEntryCount(),
				statement.getCreatedAt());
	}

}
