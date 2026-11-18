package com.wallet.api.ledger;

import com.wallet.api.account.Account;
import com.wallet.api.common.Currency;
import com.wallet.api.common.Money;

public record BalanceResponse(String accountNumber, Currency currency, String balance) {

	public static BalanceResponse from(Account account, long balanceMinor) {
		return new BalanceResponse(account.getAccountNumber(), account.getCurrency(), Money.format(balanceMinor));
	}
}
