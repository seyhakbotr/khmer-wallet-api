package com.wallet.api.transfer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

	Optional<WalletTransaction> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

}
