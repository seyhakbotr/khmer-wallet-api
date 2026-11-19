package com.wallet.api.statement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StatementRepository extends JpaRepository<Statement, Long> {

	boolean existsByAccountIdAndStatementDate(Long accountId, LocalDate statementDate);

	Optional<Statement> findByAccountIdAndStatementDate(Long accountId, LocalDate statementDate);

	List<Statement> findByAccountIdOrderByStatementDateDesc(Long accountId);

}
