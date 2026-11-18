package com.wallet.api.ledger;

import com.wallet.api.account.Account;
import com.wallet.api.account.AccountRepository;
import com.wallet.api.common.Money;
import com.wallet.api.common.error.NotFoundException;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {
    private final AccountRepository accounts;
    private final LedgerEntryRepository ledger;
    public LedgerService(AccountRepository accounts, LedgerEntryRepository ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }
    @Transactional
    public MovementResponse deposit(String phone, String accountNumber, String amountText) {
        Account account = loadOwnedAccount(phone, accountNumber);
        Money moneyAmount = Money.parse(amountText, account.getCurrency());
        LedgerEntry ledgerEntry = ledger.save(LedgerEntry.deposit(account,moneyAmount));
        long balance = ledger.sumAmountMinorByAccountId(account.getId());
        return MovementResponse.from(ledgerEntry,moneyAmount,balance);

    }

    private @NonNull Account loadOwnedAccount(String phone, String accountNumber) {
        Account account = accounts.findAccountByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        if (!account.getUser().getPhone().equals(phone)) {
            throw new NotFoundException("Account not found");
        }
        return account;
    }
}
