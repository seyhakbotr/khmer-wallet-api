package com.wallet.api.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.api.common.error.ConflictException;
import com.wallet.api.security.JwtService;
import com.wallet.api.user.User;
import com.wallet.api.user.UserRepository;
import com.wallet.api.user.UserResponse;

@Service
public class AuthService {

	private final UserRepository users;

	private final PasswordEncoder passwordEncoder;

	private final AuthenticationManager authenticationManager;

	private final JwtService jwtService;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager, JwtService jwtService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		String phone = request.phone().trim();
		if (users.existsByPhone(phone)) {
			throw new ConflictException("Phone number is already registered");
		}

		User user = new User(request.fullName().trim(), phone, passwordEncoder.encode(request.password()));
		try {
			// Flush now so a concurrent duplicate registration fails inside this method,
			// where we can translate it into a clean 409 instead of a 500.
			users.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException ex) {
			throw new ConflictException("Phone number is already registered");
		}
		return UserResponse.from(user);
	}

	public AuthResponse login(LoginRequest request) {
		String phone = request.phone().trim();
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(phone, request.password()));
		}
		catch (AuthenticationException ex) {
			throw new BadCredentialsException("Invalid phone or password");
		}
		return new AuthResponse(jwtService.generateToken(phone), "Bearer", jwtService.expirationSeconds());
	}
}
