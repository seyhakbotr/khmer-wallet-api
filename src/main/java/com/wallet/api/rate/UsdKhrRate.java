package com.wallet.api.rate;

import java.time.Instant;
import java.time.LocalDate;

import com.wallet.api.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A published USD/KHR rate, expressed as KHR sen per 1 USD. Keeping the rate in minor
 * units means rate maths never falls back to floating point.
 *
 * <p>Rates are dated: the current rate is the newest row whose {@code effectiveFrom} is
 * today or earlier. Admins may correct a date in place; users can only read.</p>
 */
@Entity
@Table(name = "usd_khr_rate",
		uniqueConstraints = @UniqueConstraint(name = "uk_usd_khr_rate_effective_from",
				columnNames = "effective_from"))
public class UsdKhrRate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "rate_minor", nullable = false)
	private long rateMinor;

	@Column(name = "effective_from", nullable = false, updatable = false)
	private LocalDate effectiveFrom;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "set_by_user_id")
	private User setBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected UsdKhrRate() {
		// for JPA
	}

	private UsdKhrRate(long rateMinor, LocalDate effectiveFrom, User setBy) {
		if (rateMinor <= 0) {
			throw new IllegalArgumentException("rate must be positive");
		}
		this.rateMinor = rateMinor;
		this.effectiveFrom = effectiveFrom;
		this.setBy = setBy;
		this.createdAt = Instant.now();
	}

	public static UsdKhrRate of(long rateMinor, LocalDate effectiveFrom, User setBy) {
		return new UsdKhrRate(rateMinor, effectiveFrom, setBy);
	}

	public void update(long rateMinor, User setBy) {
		if (rateMinor <= 0) {
			throw new IllegalArgumentException("rate must be positive");
		}
		this.rateMinor = rateMinor;
		this.setBy = setBy;
	}

	public Long getId() {
		return id;
	}

	public long getRateMinor() {
		return rateMinor;
	}

	public LocalDate getEffectiveFrom() {
		return effectiveFrom;
	}

	public User getSetBy() {
		return setBy;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
