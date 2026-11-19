package com.wallet.api.transfer;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.wallet.api.common.Currency;

/**
 * Daily outgoing-transfer cap per currency, in minor units.
 *
 * <p>Example: {@code app.transfers.daily-limits.USD: 100000} is a $1,000.00 daily limit.</p>
 */
@ConfigurationProperties(prefix = "app.transfers")
public record TransferLimitProperties(Map<Currency, Long> dailyLimits) {

	public TransferLimitProperties {
		dailyLimits = dailyLimits == null ? Map.of() : Map.copyOf(dailyLimits);
	}

	public long dailyLimitMinor(Currency currency) {
		return dailyLimits.getOrDefault(currency, Long.MAX_VALUE);
	}

}
