package com.wallet.api.rate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rates")
public class RateController {

	private final RateService rateService;

	public RateController(RateService rateService) {
		this.rateService = rateService;
	}

	@GetMapping("/usd-khr")
	public RateResponse usdKhrRate() {
		return rateService.current();
	}

}
