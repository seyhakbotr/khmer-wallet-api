package com.wallet.api.rate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/rates")
@Tag(name = "Rates", description = "Read the current USD/KHR rate")
public class RateController {

	private final RateService rateService;

	public RateController(RateService rateService) {
		this.rateService = rateService;
	}

	@GetMapping("/usd-khr")
	@Operation(summary = "Get the newest USD/KHR rate effective today")
	public RateResponse usdKhrRate() {
		return rateService.current();
	}

}
