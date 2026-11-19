package com.wallet.api.ledger;

/**
 * What moved. The direction of the movement is the sign of {@code amount_minor};
 * this label says which kind of movement it was.
 */
public enum EntryType {

	DEPOSIT,

	WITHDRAWAL,

	TRANSFER_IN,

	TRANSFER_OUT;

	boolean matches(long amountMinor) {
		return switch (this) {
			case DEPOSIT, TRANSFER_IN -> amountMinor > 0;
			case WITHDRAWAL, TRANSFER_OUT -> amountMinor < 0;
		};
	}

	boolean isTransfer() {
		return this == TRANSFER_IN || this == TRANSFER_OUT;
	}

}
