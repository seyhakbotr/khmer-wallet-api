package com.wallet.api.user;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.api.common.error.NotFoundException;

@RestController
@RequestMapping("/api/me")
public class MeController {

	private final UserRepository users;

	public MeController(UserRepository users) {
		this.users = users;
	}

	@GetMapping
	public UserResponse me(Authentication authentication) {
		return users.findByPhone(authentication.getName())
				.map(UserResponse::from)
				.orElseThrow(() -> new NotFoundException("Authenticated user no longer exists"));
	}
}
