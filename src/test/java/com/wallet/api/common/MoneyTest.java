package com.wallet.api.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

	@Test
	void addsAmountsOfTheSameCurrency() {
		Money tenDollars = Money.of(1_000, Currency.USD);
		Money twoFifty = Money.of(250, Currency.USD);

		assertThat(tenDollars.plus(twoFifty)).isEqualTo(Money.of(1_250, Currency.USD));
	}

	@Test
	void subtractsAmountsOfTheSameCurrency() {
		Money tenDollars = Money.of(1_000, Currency.USD);
		Money twoFifty = Money.of(250, Currency.USD);

		assertThat(tenDollars.minus(twoFifty)).isEqualTo(Money.of(750, Currency.USD));
	}

	@Test
	void rejectsMixingCurrencies() {
		Money usd = Money.of(1_000, Currency.USD);
		Money khr = Money.of(1_000, Currency.KHR);

		assertThatThrownBy(() -> usd.plus(khr))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("currency mismatch");
	}

	@Test
	void rejectsNegativeAmounts() {
		assertThatThrownBy(() -> Money.of(-1, Currency.USD))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsSubtractionThatWouldGoNegative() {
		Money five = Money.of(500, Currency.USD);

		assertThatThrownBy(() -> five.minus(Money.of(600, Currency.USD)))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void formatsAmountsWithTwoDecimals() {
		assertThat(Money.of(1_025, Currency.USD)).hasToString("10.25 USD");
		assertThat(Money.of(50, Currency.KHR)).hasToString("0.50 KHR");
	}
}
