package com.wallet.api.statement;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Statements", description = "Generated end-of-day statements")
public class StatementController {

	private final StatementService statementService;

	public StatementController(StatementService statementService) {
		this.statementService = statementService;
	}

	@GetMapping("/{accountNumber}/statements")
	@Operation(summary = "List my statements, newest first")
	public List<StatementResponse> statements(Authentication authentication, @PathVariable String accountNumber) {
		return statementService.listFor(authentication.getName(), accountNumber);
	}

}
