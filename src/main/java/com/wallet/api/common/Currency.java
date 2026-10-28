package com.wallet.api.common;

/**
 * Currencies supported by the wallet.
 *
 * <p>Both currencies are stored in minor units (1/100 of the base unit):
 * USD in cents, KHR in sen. Amounts are always integers — never floating point.</p>
 */
public enum Currency {
	USD,
	KHR
}
