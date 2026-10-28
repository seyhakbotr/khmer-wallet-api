package com.wallet.api.common;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * An immutable money amount in minor units of a single currency.
 *
 * <p>Money is never represented as {@code double}/{@code float}: storing e.g. USD as
 * cents keeps arithmetic exact. A {@code Money} can only be combined with another
 * {@code Money} of the same currency.</p>
 */
public record Money(long amountMinor, Currency currency) {

	public Money {
		Objects.requireNonNull(currency, "currency is required");
		if (amountMinor < 0) {
			throw new IllegalArgumentException("amountMinor must not be negative");
		}
	}

	public static Money of(long amountMinor, Currency currency) {
		return new Money(amountMinor, currency);
	}

	public Money plus(Money other) {
		return new Money(Math.addExact(amountMinor, sameCurrency(other).amountMinor), currency);
	}

	public Money minus(Money other) {
		long result = amountMinor - sameCurrency(other).amountMinor;
		if (result < 0) {
			throw new IllegalArgumentException("result would be negative: " + this + " - " + other);
		}
		return new Money(result, currency);
	}

	private Money sameCurrency(Money other) {
		Objects.requireNonNull(other, "other is required");
		if (currency != other.currency) {
			throw new IllegalArgumentException("currency mismatch: " + currency + " vs " + other.currency);
		}
		return other;
	}

	@Override
	public String toString() {
		return BigDecimal.valueOf(amountMinor, 2).toPlainString() + " " + currency;
	}
}
