package com.wallet.api.ledger;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/accounts")
public class LedgerController {
    private final LedgerService ledgerService;
    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }
    @PostMapping("/{accountNumber}/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse deposit(
            Authentication authentication,
            @PathVariable String accountNumber,
            @Valid @RequestBody MovementRequest request) {
        return ledgerService.deposit(authentication.getName(), accountNumber, request.amount());
    }
}
