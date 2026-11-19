package com.wallet.api.transfer;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.wallet.api.common.Currency;

import static org.assertj.core.api.Assertions.assertThat;

class TransferLimitPropertiesTest {

	@Test
	void returnsTheConfiguredLimitForACurrency() {
		TransferLimitProperties limits = new TransferLimitProperties(Map.of(Currency.USD, 10_000L));

		assertThat(limits.dailyLimitMinor(Currency.USD)).isEqualTo(10_000L);
	}

	@Test
	void returnsNoLimitForAnUnconfiguredCurrency() {
		TransferLimitProperties limits = new TransferLimitProperties(Map.of(Currency.USD, 10_000L));

		assertThat(limits.dailyLimitMinor(Currency.KHR)).isEqualTo(Long.MAX_VALUE);
	}

	@Test
	void treatsAMissingMapAsNoLimits() {
		TransferLimitProperties limits = new TransferLimitProperties(null);

		assertThat(limits.dailyLimitMinor(Currency.USD)).isEqualTo(Long.MAX_VALUE);
	}

}
