package com.wallet.api.rate;

import java.time.LocalDate;

import com.wallet.api.common.Money;

public record RateResponse(String pair, String rate, LocalDate effectiveFrom) {

	public static RateResponse from(UsdKhrRate rate) {
		return new RateResponse("USD/KHR", Money.format(rate.getRateMinor()), rate.getEffectiveFrom());
	}

}
