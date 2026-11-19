package com.wallet.api.ledger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.api.account.Account;
import com.wallet.api.account.AccountRepository;
import com.wallet.api.audit.AuditLogService;
import com.wallet.api.common.Money;
import com.wallet.api.common.error.BusinessRuleException;
import com.wallet.api.common.error.NotFoundException;

@Service
public class LedgerService {

	private final AccountRepository accounts;

	private final LedgerEntryRepository ledger;

	private final AuditLogService auditLog;

	public LedgerService(AccountRepository accounts, LedgerEntryRepository ledger, AuditLogService auditLog) {
		this.accounts = accounts;
		this.ledger = ledger;
		this.auditLog = auditLog;
	}

	@Transactional
	public MovementResponse deposit(String phone, String accountNumber, String amountText) {
		Account account = lockOwnedAccount(phone, accountNumber);
		Money amount = Money.parse(amountText, account.getCurrency());

		LedgerEntry entry = ledger.save(LedgerEntry.deposit(account, amount));
		auditLog.record(account.getUser(), "DEPOSIT", "LEDGER_ENTRY", entry.getId(),
				format(amount) + " into " + account.getAccountNumber());
		long balance = ledger.sumAmountMinorByAccountId(account.getId());
		return MovementResponse.from(entry, amount, balance);
	}

	@Transactional
	public MovementResponse withdraw(String phone, String accountNumber, String amountText) {
		Account account = lockOwnedAccount(phone, accountNumber);
		Money amount = Money.parse(amountText, account.getCurrency());

		long balance = ledger.sumAmountMinorByAccountId(account.getId());
		if (amount.amountMinor() > balance) {
			throw new BusinessRuleException("Insufficient funds");
		}

		LedgerEntry entry = ledger.save(LedgerEntry.withdrawal(account, amount));
		auditLog.record(account.getUser(), "WITHDRAWAL", "LEDGER_ENTRY", entry.getId(),
				format(amount) + " from " + account.getAccountNumber());
		long newBalance = ledger.sumAmountMinorByAccountId(account.getId());
		return MovementResponse.from(entry, amount, newBalance);
	}

	@Transactional(readOnly = true)
	public BalanceResponse balance(String phone, String accountNumber) {
		Account account = loadOwnedAccount(phone, accountNumber);
		long balance = ledger.sumAmountMinorByAccountId(account.getId());
		return BalanceResponse.from(account, balance);
	}

	private Account loadOwnedAccount(String phone, String accountNumber) {
		Account account = accounts.findAccountByAccountNumber(accountNumber)
				.orElseThrow(() -> new NotFoundException("Account not found"));
		if (!account.getUser().getPhone().equals(phone)) {
			throw new NotFoundException("Account not found");
		}
		return account;
	}

	private Account lockOwnedAccount(String phone, String accountNumber) {
		Account account = accounts.findAccountByAccountNumberForUpdate(accountNumber)
				.orElseThrow(() -> new NotFoundException("Account not found"));
		if (!account.getUser().getPhone().equals(phone)) {
			throw new NotFoundException("Account not found");
		}
		return account;
	}

	private String format(Money amount) {
		return Money.format(amount) + " " + amount.currency();
	}

}
