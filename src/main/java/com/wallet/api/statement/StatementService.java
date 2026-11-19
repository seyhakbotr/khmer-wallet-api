package com.wallet.api.statement;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.api.account.Account;
import com.wallet.api.account.AccountRepository;
import com.wallet.api.common.error.NotFoundException;
import com.wallet.api.ledger.EntryType;
import com.wallet.api.ledger.LedgerEntryRepository;

@Service
public class StatementService {

	private final AccountRepository accounts;

	private final LedgerEntryRepository ledger;

	private final StatementRepository statements;

	private final Clock clock;

	public StatementService(AccountRepository accounts, LedgerEntryRepository ledger, StatementRepository statements,
			Clock clock) {
		this.accounts = accounts;
		this.ledger = ledger;
		this.statements = statements;
		this.clock = clock;
	}

	/**
	 * Creates one statement per account for {@code date}, skipping accounts that were
	 * created after that day and statements that already exist. Safe to re-run.
	 */
	@Transactional
	public int generateDailyStatements(LocalDate date) {
		ZoneId zone = clock.getZone();
		Instant start = date.atStartOfDay(zone).toInstant();
		Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();

		int generated = 0;
		for (Account account : accounts.findAll()) {
			if (!account.getCreatedAt().isBefore(end)
					|| statements.existsByAccountIdAndStatementDate(account.getId(), date)) {
				continue;
			}

			long opening = ledger.sumAmountMinorBefore(account.getId(), start);
			long deposits = ledger.sumAmountMinorByEntryTypeBetween(account.getId(), EntryType.DEPOSIT, start, end);
			long withdrawals = ledger.sumAmountMinorByEntryTypeBetween(account.getId(), EntryType.WITHDRAWAL, start,
					end);
			long transfersIn = ledger.sumAmountMinorByEntryTypeBetween(account.getId(), EntryType.TRANSFER_IN, start,
					end);
			long transfersOut = ledger.sumAmountMinorByEntryTypeBetween(account.getId(), EntryType.TRANSFER_OUT, start,
					end);
			long closing = opening + deposits + withdrawals + transfersIn + transfersOut;
			long entryCount = ledger.countBetween(account.getId(), start, end);

			statements.save(Statement.of(account, date, opening, closing, deposits, Math.negateExact(withdrawals),
					transfersIn, Math.negateExact(transfersOut), entryCount));
			generated++;
		}
		return generated;
	}

	@Transactional(readOnly = true)
	public List<StatementResponse> listFor(String phone, String accountNumber) {
		Account account = accounts.findAccountByAccountNumber(accountNumber)
				.orElseThrow(() -> new NotFoundException("Account not found"));
		if (!account.getUser().getPhone().equals(phone)) {
			throw new NotFoundException("Account not found");
		}
		return statements.findByAccountIdOrderByStatementDateDesc(account.getId())
			.stream()
			.map(StatementResponse::from)
			.toList();
	}

}
