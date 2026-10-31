package com.wallet.api.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey key;
	private final long expirationMillis;

	public JwtService(JwtProperties properties) {
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
		this.expirationMillis = properties.expirationMinutes() * 60_000L;
	}

	public String generateToken(String phone) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(phone)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(expirationMillis)))
				.signWith(key)
				.compact();
	}

	public String extractSubject(String token) {
		return parseClaims(token).getSubject();
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		try {
			Claims claims = parseClaims(token);
			return claims.getSubject().equals(userDetails.getUsername())
					&& claims.getExpiration().after(new Date());
		}
		catch (RuntimeException ex) {
			return false;
		}
	}

	public long expirationSeconds() {
		return expirationMillis / 1000;
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
