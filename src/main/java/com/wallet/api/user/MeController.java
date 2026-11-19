package com.wallet.api.user;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.api.common.error.NotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Users", description = "Current user")
public class MeController {

	private final UserRepository users;

	public MeController(UserRepository users) {
		this.users = users;
	}

	@GetMapping
	@Operation(summary = "Get the authenticated user")
	public UserResponse me(Authentication authentication) {
		return users.findByPhone(authentication.getName())
				.map(UserResponse::from)
				.orElseThrow(() -> new NotFoundException("Authenticated user no longer exists"));
	}
}
