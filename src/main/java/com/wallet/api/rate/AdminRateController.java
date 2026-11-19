package com.wallet.api.rate;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/usd-khr-rates")
public class AdminRateController {

	private final RateService rateService;

	public AdminRateController(RateService rateService) {
		this.rateService = rateService;
	}

	@PutMapping
	public RateResponse setRate(Authentication authentication, @Valid @RequestBody SetRateRequest request) {
		return rateService.set(authentication.getName(), request);
	}

}
