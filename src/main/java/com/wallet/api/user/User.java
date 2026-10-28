package com.wallet.api.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "full_name", nullable = false, length = 120)
	private String fullName;

	/** Login identifier: Cambodian phone number, e.g. {@code 012345678}. */
	@Column(nullable = false, unique = true, length = 20)
	private String phone;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected User() {
		// for JPA
	}

	public User(String fullName, String phone, String passwordHash) {
		this.fullName = fullName;
		this.phone = phone;
		this.passwordHash = passwordHash;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public String getPhone() {
		return phone;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
