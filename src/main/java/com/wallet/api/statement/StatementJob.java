package com.wallet.api.statement;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StatementJob {

	private static final Logger log = LoggerFactory.getLogger(StatementJob.class);

	private final StatementService statements;

	private final Clock clock;

	public StatementJob(StatementService statements, Clock clock) {
		this.statements = statements;
		this.clock = clock;
	}

	@Scheduled(cron = "${app.statements.cron}", zone = "${app.zone}")
	public void generateYesterday() {
		LocalDate yesterday = LocalDate.now(clock).minusDays(1);
		int generated = statements.generateDailyStatements(yesterday);
		log.info("Generated {} statement(s) for {}", generated, yesterday);
	}

}
