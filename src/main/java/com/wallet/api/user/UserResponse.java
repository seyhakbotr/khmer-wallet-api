package com.wallet.api.user;

import java.time.Instant;

public record UserResponse(Long id, String fullName, String phone, Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getFullName(), user.getPhone(), user.getCreatedAt());
	}
}
