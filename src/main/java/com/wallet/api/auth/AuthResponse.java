package com.wallet.api.auth;

public record AuthResponse(String token, String tokenType, long expiresInSeconds) {
}
