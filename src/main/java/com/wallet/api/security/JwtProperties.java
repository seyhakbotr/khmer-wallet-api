package com.wallet.api.security;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMinutes) {

	public JwtProperties {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalArgumentException("app.jwt.secret must be at least 32 bytes (HS256 requirement)");
		}
		if (expirationMinutes <= 0) {
			throw new IllegalArgumentException("app.jwt.expiration-minutes must be positive");
		}
	}
}
