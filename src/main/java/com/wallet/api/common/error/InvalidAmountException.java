package com.wallet.api.common.error;

public class InvalidAmountException extends RuntimeException {

	private final String field;

	public InvalidAmountException(String message) {
		this("amount", message);
	}

	public InvalidAmountException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}

}
