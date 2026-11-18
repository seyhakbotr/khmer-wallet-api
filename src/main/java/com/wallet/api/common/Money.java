package com.wallet.api.common;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

import com.wallet.api.common.error.InvalidAmountException;

/**
 * An immutable money amount in minor units of a single currency.
 *
 * <p>Money is never represented as {@code double}/{@code float}: storing e.g. USD as
 * cents keeps arithmetic exact. A {@code Money} can only be combined with another
 * {@code Money} of the same currency.</p>
 */
public record Money(long amountMinor, Currency currency) {

	private static final Pattern AMOUNT_PATTERN = Pattern.compile("\\d+(\\.\\d{1,2})?");
	private static final long MAX_PER_OPERATION_MINOR = 100_000_000L;

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

	public static Money parse(String amount, Currency currency) {
		if (amount == null || amount.isBlank()) {
			throw new InvalidAmountException("amount is required");
		}

		String trimmed = amount.strip();
		if (trimmed.startsWith("-") || trimmed.startsWith("+")) {
			throw new InvalidAmountException("amount must not be signed");
		}
		if (!AMOUNT_PATTERN.matcher(trimmed).matches()) {
			throw new InvalidAmountException("invalid amount format: " + trimmed);
		}

		BigDecimal parsed = new BigDecimal(trimmed);
		if (parsed.scale() > 2) {
			throw new InvalidAmountException("amount must have at most 2 decimal places");
		}

		long amountMinor;
		try {
			amountMinor = parsed.movePointRight(2).longValueExact();
		} catch (ArithmeticException ex) {
			throw new InvalidAmountException("amount is too large");
		}

		if (amountMinor == 0) {
			throw new InvalidAmountException("amount must be greater than zero");
		}
		if (amountMinor > MAX_PER_OPERATION_MINOR) {
			throw new InvalidAmountException("amount exceeds the per-operation maximum");
		}

		return new Money(amountMinor, currency);
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
