package com.wallet.api.ledger;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
public class LedgerController {

	private final LedgerService ledgerService;

	public LedgerController(LedgerService ledgerService) {
		this.ledgerService = ledgerService;
	}

	@PostMapping("/{accountNumber}/deposits")
	@ResponseStatus(HttpStatus.CREATED)
	public MovementResponse deposit(Authentication authentication, @PathVariable String accountNumber,
			@Valid @RequestBody MovementRequest request) {
		return ledgerService.deposit(authentication.getName(), accountNumber, request.amount());
	}

	@PostMapping("/{accountNumber}/withdrawals")
	@ResponseStatus(HttpStatus.CREATED)
	public MovementResponse withdraw(Authentication authentication, @PathVariable String accountNumber,
			@Valid @RequestBody MovementRequest request) {
		return ledgerService.withdraw(authentication.getName(), accountNumber, request.amount());
	}

	@GetMapping("/{accountNumber}/balance")
	public BalanceResponse balance(Authentication authentication, @PathVariable String accountNumber) {
		return ledgerService.balance(authentication.getName(), accountNumber);
	}
}
