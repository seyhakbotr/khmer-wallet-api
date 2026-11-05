package com.wallet.api.account;

import com.wallet.api.common.Currency;

import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(@NotNull Currency currency) {
}
