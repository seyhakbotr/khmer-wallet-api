package com.wallet.api.account;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.api.common.Currency;
import com.wallet.api.common.error.ConflictException;
import com.wallet.api.common.error.NotFoundException;
import com.wallet.api.user.User;
import com.wallet.api.user.UserRepository;

@Service
public class AccountService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final AccountRepository accounts;

	private final UserRepository users;

	public AccountService(AccountRepository accounts, UserRepository users) {
		this.accounts = accounts;
		this.users = users;
	}

	@Transactional
	public AccountResponse create(String phone, Currency currency) {
		User user = users.findByPhone(phone)
				.orElseThrow(() -> new NotFoundException("User not found"));

		if (accounts.existsByUserIdAndCurrency(user.getId(), currency)) {
			throw new ConflictException("User already has a " + currency + " account");
		}

		Account account = new Account(user, generateAccountNumber(), currency);
		try {
			accounts.saveAndFlush(account);
		}
		catch (DataIntegrityViolationException ex) {
			// Covers a concurrent duplicate create or the (unlikely) account-number collision.
			throw new ConflictException("Could not create account, please retry");
		}
		return AccountResponse.from(account);
	}

	@Transactional(readOnly = true)
	public List<AccountResponse> listFor(String phone) {
		User user = users.findByPhone(phone)
				.orElseThrow(() -> new NotFoundException("User not found"));
		return accounts.findByUserIdOrderByIdAsc(user.getId())
				.stream()
				.map(AccountResponse::from)
				.toList();
	}

	private String generateAccountNumber() {
		for (int attempt = 0; attempt < 10; attempt++) {
			String candidate = "9" + String.format("%09d", RANDOM.nextInt(1_000_000_000));
			if (!accounts.existsByAccountNumber(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("Could not generate a unique account number");
	}
}
