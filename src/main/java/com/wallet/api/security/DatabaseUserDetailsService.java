package com.wallet.api.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.wallet.api.user.User;
import com.wallet.api.user.UserRepository;

/**
 * Tells Spring Security how to look up a user: the phone number is the username.
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

	private final UserRepository users;

	public DatabaseUserDetailsService(UserRepository users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
		User user = users.findByPhone(phone)
				.orElseThrow(() -> new UsernameNotFoundException("No user with phone " + phone));
		return org.springframework.security.core.userdetails.User.withUsername(user.getPhone())
				.password(user.getPasswordHash())
				.roles("USER")
				.build();
	}
}
