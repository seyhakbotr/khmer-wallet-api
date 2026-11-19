package com.wallet.api.transfer;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

	private final TransferService transferService;

	public TransferController(TransferService transferService) {
		this.transferService = transferService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TransferResponse transfer(Authentication authentication,
			@RequestHeader("Idempotency-Key") String idempotencyKey, @Valid @RequestBody TransferRequest request) {
		return transferService.transfer(authentication.getName(), idempotencyKey, request);
	}

}
