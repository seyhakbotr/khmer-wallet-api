package com.wallet.api.rate;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.api.common.Currency;
import com.wallet.api.common.Money;
import com.wallet.api.common.error.InvalidAmountException;
import com.wallet.api.common.error.NotFoundException;
import com.wallet.api.user.User;
import com.wallet.api.user.UserRepository;

@Service
public class RateService {

	private final UsdKhrRateRepository rates;

	private final UserRepository users;

	private final Clock clock;

	public RateService(UsdKhrRateRepository rates, UserRepository users, Clock clock) {
		this.rates = rates;
		this.users = users;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public RateResponse current() {
		LocalDate today = LocalDate.now(clock);
		return rates.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(today)
			.map(RateResponse::from)
			.orElseThrow(() -> new NotFoundException("USD/KHR rate is not configured"));
	}

	/** Upserts the rate for one effective date, so an admin can correct a published rate. */
	@Transactional
	public RateResponse set(String phone, SetRateRequest request) {
		User admin = users.findByPhone(phone)
				.orElseThrow(() -> new NotFoundException("User not found"));
		long rateMinor = parseRate(request.rate());

		UsdKhrRate rate = rates.findByEffectiveFrom(request.effectiveFrom())
			.map(existing -> {
				existing.update(rateMinor, admin);
				return existing;
			})
			.orElseGet(() -> UsdKhrRate.of(rateMinor, request.effectiveFrom(), admin));
		return RateResponse.from(rates.save(rate));
	}

	private long parseRate(String rate) {
		try {
			return Money.parse(rate, Currency.KHR).amountMinor();
		}
		catch (InvalidAmountException ex) {
			throw new InvalidAmountException("rate", ex.getMessage());
		}
	}

}
