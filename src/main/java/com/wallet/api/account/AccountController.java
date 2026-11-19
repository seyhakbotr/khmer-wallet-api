package com.wallet.api.account;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Create and list wallet accounts")
public class AccountController {

	private final AccountService accountService;

	public AccountController(AccountService accountService) {
		this.accountService = accountService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a USD or KHR account")
	public AccountResponse create(Authentication authentication, @Valid @RequestBody CreateAccountRequest request) {
		return accountService.create(authentication.getName(), request.currency());
	}

	@GetMapping
	@Operation(summary = "List my accounts")
	public List<AccountResponse> list(Authentication authentication) {
		return accountService.listFor(authentication.getName());
	}
}
