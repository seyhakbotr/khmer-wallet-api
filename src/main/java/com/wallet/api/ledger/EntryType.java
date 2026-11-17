package com.wallet.api.ledger;

/**
 * What moved. The direction of the movement is the sign of {@code amount_minor};
 * this label says which kind of movement it was.
 *
 * <p>Phase 3 adds {@code TRANSFER_IN} and {@code TRANSFER_OUT}.</p>
 */
public enum EntryType {

	DEPOSIT,

	WITHDRAWAL;

	boolean matches(long amountMinor) {
		return switch (this) {
			case DEPOSIT -> amountMinor > 0;
			case WITHDRAWAL -> amountMinor < 0;
		};
	}

}
