package com.wallet.api.ledger;

import com.wallet.api.common.Currency;
import com.wallet.api.common.Money;

import java.math.BigDecimal;
import java.time.Instant;

public record MovementResponse(Long entryId, String accountNumber, Currency currency, String amount, String balance,
                               Instant createdAt) {
    public static MovementResponse from(LedgerEntry entry, Money amount, long balanceMinor) {
        return new MovementResponse(
                entry.getId(),
                entry.getAccount().getAccountNumber(),
                entry.getAccount().getCurrency(),
                BigDecimal.valueOf(amount.amountMinor(),2).toPlainString(),
                BigDecimal.valueOf(balanceMinor,2).toPlainString(),
                entry.getCreatedAt()
        );
    }
}
