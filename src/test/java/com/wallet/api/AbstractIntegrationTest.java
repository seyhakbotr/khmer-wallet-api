package com.wallet.api;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for integration tests: a real Postgres (Testcontainers) plus MockMvc
 * wired through the actual Spring Security filter chain.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	protected MockMvc mockMvc;

	@BeforeEach
	void setUpMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(springSecurity())
				.build();
	}

	protected String uniquePhone() {
		return "0" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
	}

	protected ResultActions register(String phone, String fullName, String password) throws Exception {
		String body = """
				{"fullName": "%s", "phone": "%s", "password": "%s"}
				""".formatted(fullName, phone, password);
		return mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	protected String loginAndGetToken(String phone, String password) throws Exception {
		String body = """
				{"phone": "%s", "password": "%s"}
				""".formatted(phone, password);
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
	}

	/** Registers a fresh random user and returns a valid bearer token. */
	protected String registerAndLogin() throws Exception {
		String phone = uniquePhone();
		register(phone, "Test User", "password123").andExpect(status().isCreated());
		return loginAndGetToken(phone, "password123");
	}
}
