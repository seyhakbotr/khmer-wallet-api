package com.wallet.api.common.error;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Single, predictable error shape for every failure the API returns.
 */
public record ApiError(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		Map<String, String> fieldErrors) {

	public static ApiError of(HttpStatus status, String message, String path) {
		return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, Map.of());
	}

	public static ApiError validation(String message, String path, Map<String, String> fieldErrors) {
		return new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
				message, path, fieldErrors);
	}
}
