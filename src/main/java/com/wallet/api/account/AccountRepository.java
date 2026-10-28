package com.wallet.api.account;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wallet.api.common.Currency;

public interface AccountRepository extends JpaRepository<Account, Long> {

	List<Account> findByUserIdOrderByIdAsc(Long userId);

	boolean existsByAccountNumber(String accountNumber);

	boolean existsByUserIdAndCurrency(Long userId, Currency currency);
}
