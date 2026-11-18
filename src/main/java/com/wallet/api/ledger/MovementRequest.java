package com.wallet.api.ledger;

import jakarta.validation.constraints.NotBlank;

public record MovementRequest(@NotBlank String amount) {
}
