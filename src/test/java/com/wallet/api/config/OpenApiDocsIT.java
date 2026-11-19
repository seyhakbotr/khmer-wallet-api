package com.wallet.api.config;

import org.junit.jupiter.api.Test;

import com.wallet.api.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiDocsIT extends AbstractIntegrationTest {

	@Test
	void apiDocsArePublicAndDescribeTheWallet() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.info.title").value("Khmer Wallet API"))
			.andExpect(jsonPath("$.paths['/api/transfers']").exists())
			.andExpect(jsonPath("$.paths['/api/auth/login'].post.security").isEmpty())
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
	}

	@Test
	void swaggerUiIsPublic() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
			.andExpect(status().is3xxRedirection());

		mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk());
	}

}
