package com.wallet.api.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.wallet.api.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends AbstractIntegrationTest {

	@Test
	void registersNewUser() throws Exception {
		String phone = uniquePhone();

		register(phone, "Dara Sok", "password123")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.fullName").value("Dara Sok"))
			.andExpect(jsonPath("$.phone").value(phone))
			.andExpect(jsonPath("$.createdAt").exists())
			.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void rejectsDuplicatePhone() throws Exception {
		String phone = uniquePhone();
		register(phone, "First User", "password123").andExpect(status().isCreated());

		register(phone, "Second User", "password123")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.message").value("Phone number is already registered"));
	}

	@Test
	void rejectsInvalidPhone() throws Exception {
		register("12345", "Bad Phone", "password123")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.phone").exists());
	}

	@Test
	void rejectsShortPassword() throws Exception {
		register(uniquePhone(), "Short Pass", "short")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.password").exists());
	}

	@Test
	void rejectsBlankFullName() throws Exception {
		register(uniquePhone(), "  ", "password123")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.fullName").exists());
	}

	@Test
	void rejectsMalformedJson() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{not-json"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Malformed JSON request"));
	}

	@Test
	void loginReturnsToken() throws Exception {
		String phone = uniquePhone();
		register(phone, "Login User", "password123").andExpect(status().isCreated());

		String body = """
				{"phone": "%s", "password": "password123"}
				""".formatted(phone);
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresInSeconds").isNumber());
	}

	@Test
	void loginWithWrongPasswordReturns401() throws Exception {
		String phone = uniquePhone();
		register(phone, "Wrong Pass", "password123").andExpect(status().isCreated());

		String body = """
				{"phone": "%s", "password": "not-the-password"}
				""".formatted(phone);
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Invalid phone or password"));
	}

	@Test
	void loginWithUnknownPhoneReturns401() throws Exception {
		String body = """
				{"phone": "099999999", "password": "password123"}
				""";
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void meReturnsCurrentUser() throws Exception {
		String phone = uniquePhone();
		register(phone, "Me User", "password123").andExpect(status().isCreated());
		String token = loginAndGetToken(phone, "password123");

		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.phone").value(phone))
			.andExpect(jsonPath("$.fullName").value("Me User"));
	}

	@Test
	void meWithoutTokenReturns401() throws Exception {
		mockMvc.perform(get("/api/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void meWithInvalidTokenReturns401() throws Exception {
		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
			.andExpect(status().isUnauthorized());
	}
}
