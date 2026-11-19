package com.wallet.api;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class WalletApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(WalletApiApplication.class, args);
	}

	/** One clock for daily limits and statements, using the wallet's business time zone. */
	@Bean
	Clock clock(@Value("${app.zone:Asia/Phnom_Penh}") String zone) {
		return Clock.system(ZoneId.of(zone));
	}

}
