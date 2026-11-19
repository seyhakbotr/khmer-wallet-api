package com.wallet.api.rate;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/usd-khr-rates")
@Tag(name = "Admin", description = "ADMIN-only operations")
public class AdminRateController {

	private final RateService rateService;

	public AdminRateController(RateService rateService) {
		this.rateService = rateService;
	}

	@PutMapping
	@Operation(summary = "Publish or correct a dated USD/KHR rate")
	public RateResponse setRate(Authentication authentication, @Valid @RequestBody SetRateRequest request) {
		return rateService.set(authentication.getName(), request);
	}

}
