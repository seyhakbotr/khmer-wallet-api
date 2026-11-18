package com.wallet.api.common;

import org.junit.jupiter.api.Test;

import com.wallet.api.common.error.InvalidAmountException;

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

	@Test
	void formatsMinorUnitsAsPlainDecimalStrings() {
		assertThat(Money.format(1_050)).isEqualTo("10.50");
		assertThat(Money.format(50)).isEqualTo("0.50");
		assertThat(Money.format(0)).isEqualTo("0.00");
		assertThat(Money.format(Money.of(1_025, Currency.USD))).isEqualTo("10.25");
	}

	@Test
	void parsesDecimalStringsToMinorUnits() {
		assertThat(Money.parse("10.50", Currency.USD)).isEqualTo(Money.of(1_050, Currency.USD));
		assertThat(Money.parse("10.5", Currency.USD)).isEqualTo(Money.of(1_050, Currency.USD));
		assertThat(Money.parse("10", Currency.USD)).isEqualTo(Money.of(1_000, Currency.USD));
		assertThat(Money.parse("0.01", Currency.KHR)).isEqualTo(Money.of(1, Currency.KHR));
		assertThat(Money.parse(" 10.50 ", Currency.USD)).isEqualTo(Money.of(1_050, Currency.USD));
	}

	@Test
	void acceptsThePerOperationMaximum() {
		assertThat(Money.parse("1000000.00", Currency.USD)).isEqualTo(Money.of(100_000_000, Currency.USD));
	}

	@Test
	void rejectsBlankAmounts() {
		assertInvalidAmount(null);
		assertInvalidAmount("");
		assertInvalidAmount("   ");
	}

	@Test
	void rejectsSignedAmounts() {
		assertInvalidAmount("-5.00");
		assertInvalidAmount("+5.00");
	}

	@Test
	void rejectsMalformedAmounts() {
		assertInvalidAmount("abc");
		assertInvalidAmount("1e5");
		assertInvalidAmount("10.");
		assertInvalidAmount(".5");
	}

	@Test
	void rejectsMoreThanTwoDecimalPlaces() {
		assertInvalidAmount("10.555");
		assertInvalidAmount("0.005");
	}

	@Test
	void rejectsZeroAmounts() {
		assertInvalidAmount("0");
		assertInvalidAmount("0.00");
	}

	@Test
	void rejectsAmountsThatOverflowALong() {
		assertInvalidAmount("99999999999999999999.99");
	}

	@Test
	void rejectsAmountsAboveThePerOperationMaximum() {
		assertInvalidAmount("1000000.01");
	}

	private static void assertInvalidAmount(String amount) {
		assertThatThrownBy(() -> Money.parse(amount, Currency.USD))
			.isInstanceOf(InvalidAmountException.class);
	}
}
