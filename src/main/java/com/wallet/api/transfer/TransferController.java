package com.wallet.api.transfer;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transfers")
@Tag(name = "Transfers", description = "Double-entry transfers with idempotency")
public class TransferController {

	private final TransferService transferService;

	public TransferController(TransferService transferService) {
		this.transferService = transferService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Transfer money to another account in the same currency")
	public TransferResponse transfer(Authentication authentication,
			@Parameter(description = "Unique retry key; the same key returns the original result")
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody TransferRequest request) {
		return transferService.transfer(authentication.getName(), idempotencyKey, request);
	}

}
