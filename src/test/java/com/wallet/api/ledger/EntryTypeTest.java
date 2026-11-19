package com.wallet.api.ledger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntryTypeTest {

	@Test
	void depositsAndTransfersInArePositive() {
		assertThat(EntryType.DEPOSIT.matches(1)).isTrue();
		assertThat(EntryType.TRANSFER_IN.matches(1)).isTrue();
		assertThat(EntryType.DEPOSIT.matches(-1)).isFalse();
		assertThat(EntryType.TRANSFER_IN.matches(-1)).isFalse();
	}

	@Test
	void withdrawalsAndTransfersOutAreNegative() {
		assertThat(EntryType.WITHDRAWAL.matches(-1)).isTrue();
		assertThat(EntryType.TRANSFER_OUT.matches(-1)).isTrue();
		assertThat(EntryType.WITHDRAWAL.matches(1)).isFalse();
		assertThat(EntryType.TRANSFER_OUT.matches(1)).isFalse();
	}

	@Test
	void onlyTransferEntryTypesNeedATransaction() {
		assertThat(EntryType.TRANSFER_IN.isTransfer()).isTrue();
		assertThat(EntryType.TRANSFER_OUT.isTransfer()).isTrue();
		assertThat(EntryType.DEPOSIT.isTransfer()).isFalse();
		assertThat(EntryType.WITHDRAWAL.isTransfer()).isFalse();
	}

}
