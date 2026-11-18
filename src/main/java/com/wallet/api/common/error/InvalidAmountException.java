package com.wallet.api.common.error;

public class InvalidAmountException extends RuntimeException {

	public InvalidAmountException(String message) {
		super(message);
	}
}
