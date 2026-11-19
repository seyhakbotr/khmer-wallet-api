package com.wallet.api.statement;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class StatementController {

	private final StatementService statementService;

	public StatementController(StatementService statementService) {
		this.statementService = statementService;
	}

	@GetMapping("/{accountNumber}/statements")
	public List<StatementResponse> statements(Authentication authentication, @PathVariable String accountNumber) {
		return statementService.listFor(authentication.getName(), accountNumber);
	}

}
