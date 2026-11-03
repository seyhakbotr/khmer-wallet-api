package com.wallet.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Size(max = 120) String fullName,
		@NotBlank @Pattern(regexp = "^(?:\\+855|0)\\d{8,9}$",
				message = "must be a Cambodian phone number, e.g. 012345678 or +85512345678") String phone,
		@NotBlank @Size(min = 8, max = 72) String password) {
}
