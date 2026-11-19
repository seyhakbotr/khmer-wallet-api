package com.wallet.api.account;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wallet.api.common.Currency;

import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, Long> {

	List<Account> findByUserIdOrderByIdAsc(Long userId);

	boolean existsByAccountNumber(String accountNumber);

	boolean existsByUserIdAndCurrency(Long userId, Currency currency);

	Optional<Account> findAccountByAccountNumber(String accountNumber);

	/**
	 * Serializes money movements on the same account: every deposit, withdrawal and
	 * transfer locks its account rows before reading a balance or writing entries.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where a.accountNumber = :accountNumber")
	Optional<Account> findAccountByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where a.id = :id")
	Optional<Account> findByIdForUpdate(@Param("id") Long id);

}
