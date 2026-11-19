package com.wallet.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI walletOpenApi() {
		SecurityScheme bearerAuth = new SecurityScheme()
			.type(SecurityScheme.Type.HTTP)
			.scheme("bearer")
			.bearerFormat("JWT");

		return new OpenAPI()
			.info(new Info()
				.title("Khmer Wallet API")
				.version("1.0.0")
				.description("""
						Multi-currency wallet (USD/KHR) with an append-only ledger, double-entry transfers,
						idempotency keys, pessimistic locking, daily limits and end-of-day statements.

						Register or log in first, then paste the returned JWT into Authorize.
						"""))
			.components(new Components().addSecuritySchemes("bearerAuth", bearerAuth))
			.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
	}

}
