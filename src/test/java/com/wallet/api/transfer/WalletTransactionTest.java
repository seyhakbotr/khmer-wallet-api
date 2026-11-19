package com.wallet.api.transfer;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.wallet.api.account.Account;
import com.wallet.api.common.Currency;
import com.wallet.api.common.Money;
import com.wallet.api.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletTransactionTest {

	private final User user = new User("Test User", "012345678", "hash");

	@Test
	void recordsBothBalancesAndTheIdempotencyKey() {
		Account source = account("9000000001", 1L);
		Account destination = account("9000000002", 2L);
		Money amount = Money.of(1_234, Currency.USD);

		WalletTransaction transaction = WalletTransaction.transfer(user, "key-1", source, destination, amount,
				8_766, 1_234);

		assertThat(transaction.getUser()).isSameAs(user);
		assertThat(transaction.getIdempotencyKey()).isEqualTo("key-1");
		assertThat(transaction.getTransactionType()).isEqualTo(TransactionType.TRANSFER);
		assertThat(transaction.getAmountMinor()).isEqualTo(1_234);
		assertThat(transaction.getCurrency()).isEqualTo(Currency.USD);
		assertThat(transaction.getSourceBalanceAfterMinor()).isEqualTo(8_766);
		assertThat(transaction.getDestinationBalanceAfterMinor()).isEqualTo(1_234);
		assertThat(transaction.getCreatedAt()).isNotNull();
	}

	@Test
	void rejectsATransferToTheSameAccount() {
		Account account = account("9000000001", 1L);

		assertThatThrownBy(() -> WalletTransaction.transfer(user, "key-1", account, account,
				Money.of(100, Currency.USD), 0, 100))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("must differ");
	}

	@Test
	void rejectsAZeroAmount() {
		Account source = account("9000000001", 1L);
		Account destination = account("9000000002", 2L);

		assertThatThrownBy(() -> WalletTransaction.transfer(user, "key-1", source, destination,
				Money.of(0, Currency.USD), 0, 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("positive");
	}

	private Account account(String accountNumber, long id) {
		Account account = new Account(user, accountNumber, Currency.USD);
		ReflectionTestUtils.setField(account, "id", id);
		return account;
	}

}
