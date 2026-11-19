package com.wallet.api.transfer;

import jakarta.validation.constraints.NotBlank;

public record TransferRequest(@NotBlank String sourceAccountNumber, @NotBlank String destinationAccountNumber,
		@NotBlank String amount) {

	public TransferRequest {
		sourceAccountNumber = strip(sourceAccountNumber);
		destinationAccountNumber = strip(destinationAccountNumber);
	}

	private static String strip(String value) {
		return value == null ? null : value.strip();
	}

}
