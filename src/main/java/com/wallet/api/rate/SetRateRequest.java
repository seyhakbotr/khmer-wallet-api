package com.wallet.api.rate;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SetRateRequest(@NotBlank String rate, @NotNull LocalDate effectiveFrom) {

}
