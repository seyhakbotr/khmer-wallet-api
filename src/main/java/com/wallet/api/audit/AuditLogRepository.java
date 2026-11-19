package com.wallet.api.audit;

import org.springframework.data.repository.Repository;

/**
 * Deliberately exposes only {@code save}: an audit log that can be edited is not an audit log.
 * The database enforces the same rule with triggers.
 */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

	AuditLog save(AuditLog entry);

}
