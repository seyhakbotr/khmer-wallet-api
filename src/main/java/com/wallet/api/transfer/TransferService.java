package com.wallet.api.transfer;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.api.account.Account;
import com.wallet.api.account.AccountRepository;
import com.wallet.api.audit.AuditLogService;
import com.wallet.api.common.Money;
import com.wallet.api.common.error.BadRequestException;
import com.wallet.api.common.error.BusinessRuleException;
import com.wallet.api.common.error.ConflictException;
import com.wallet.api.common.error.NotFoundException;
import com.wallet.api.ledger.LedgerEntry;
import com.wallet.api.ledger.LedgerEntryRepository;
import com.wallet.api.user.User;
import com.wallet.api.user.UserRepository;

import jakarta.persistence.EntityManager;

@Service
public class TransferService {

	private static final Pattern IDEMPOTENCY_KEY_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{1,100}");

	private final AccountRepository accounts;

	private final LedgerEntryRepository ledger;

	private final WalletTransactionRepository transactions;

	private final UserRepository users;

	private final AuditLogService auditLog;

	private final EntityManager entityManager;

	public TransferService(AccountRepository accounts, LedgerEntryRepository ledger,
			WalletTransactionRepository transactions, UserRepository users, AuditLogService auditLog,
			EntityManager entityManager) {
		this.accounts = accounts;
		this.ledger = ledger;
		this.transactions = transactions;
		this.users = users;
		this.auditLog = auditLog;
		this.entityManager = entityManager;
	}

	@Transactional
	public TransferResponse transfer(String phone, String idempotencyKey, TransferRequest request) {
		String key = normalizeKey(idempotencyKey);
		User user = users.findByPhone(phone)
				.orElseThrow(() -> new NotFoundException("User not found"));

		lockIdempotencyKey(user.getId(), key);
		Optional<WalletTransaction> existing = transactions.findByUserIdAndIdempotencyKey(user.getId(), key);
		if (existing.isPresent()) {
			return replay(existing.get(), request);
		}

		Account source = accounts.findAccountByAccountNumber(request.sourceAccountNumber())
				.orElseThrow(() -> new NotFoundException("Account not found"));
		Account destination = accounts.findAccountByAccountNumber(request.destinationAccountNumber())
				.orElseThrow(() -> new NotFoundException("Account not found"));
		if (!source.getUser().getId().equals(user.getId())) {
			throw new NotFoundException("Account not found");
		}
		if (source.getId().equals(destination.getId())) {
			throw new BusinessRuleException("Source and destination accounts must differ");
		}

		lockInIdOrder(source.getId(), destination.getId());

		if (source.getCurrency() != destination.getCurrency()) {
			throw new BusinessRuleException("Cross-currency transfers are not supported");
		}
		Money amount = Money.parse(request.amount(), source.getCurrency());

		long sourceBalance = ledger.sumAmountMinorByAccountId(source.getId());
		long destinationBalance = ledger.sumAmountMinorByAccountId(destination.getId());
		if (amount.amountMinor() > sourceBalance) {
			throw new BusinessRuleException("Insufficient funds");
		}

		WalletTransaction transaction = WalletTransaction.transfer(user, key, source, destination, amount,
				sourceBalance - amount.amountMinor(), destinationBalance + amount.amountMinor());
		transactions.saveAndFlush(transaction);

		ledger.saveAll(List.of(
				LedgerEntry.transferOut(source, transaction, amount),
				LedgerEntry.transferIn(destination, transaction, amount)));
		auditLog.record(user, "TRANSFER", "TRANSACTION", transaction.getId(),
				Money.format(amount) + " " + amount.currency() + " from " + source.getAccountNumber() + " to "
						+ destination.getAccountNumber());

		return TransferResponse.from(transaction);
	}

	/**
	 * Serializes requests that reuse the same idempotency key. The unique constraint is the
	 * last line of defence; this lock means the second request sees the first one's row
	 * instead of racing it and hitting a constraint violation.
	 */
	private void lockIdempotencyKey(Long userId, String key) {
		entityManager.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(?1, 0))")
			.setParameter(1, userId + ":" + key)
			.getResultList();
	}

	private void lockInIdOrder(Long sourceId, Long destinationId) {
		Long first = Math.min(sourceId, destinationId);
		Long second = Math.max(sourceId, destinationId);
		accounts.findByIdForUpdate(first).orElseThrow(() -> new NotFoundException("Account not found"));
		accounts.findByIdForUpdate(second).orElseThrow(() -> new NotFoundException("Account not found"));
	}

	private TransferResponse replay(WalletTransaction transaction, TransferRequest request) {
		Money requested = Money.parse(request.amount(), transaction.getCurrency());
		boolean sameRequest = transaction.getSourceAccount().getAccountNumber().equals(request.sourceAccountNumber())
				&& transaction.getDestinationAccount().getAccountNumber().equals(request.destinationAccountNumber())
				&& transaction.getAmountMinor() == requested.amountMinor();
		if (!sameRequest) {
			throw new ConflictException("Idempotency-Key was already used with a different request");
		}
		return TransferResponse.from(transaction);
	}

	private String normalizeKey(String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new BadRequestException("Idempotency-Key header is required");
		}
		String key = idempotencyKey.strip();
		if (!IDEMPOTENCY_KEY_PATTERN.matcher(key).matches()) {
			throw new BadRequestException(
					"Idempotency-Key must be 1-100 characters of letters, digits, '.', '_', ':' or '-'");
		}
		return key;
	}

}
