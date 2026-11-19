package com.wallet.api.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.api.user.User;

@Service
public class AuditLogService {

	private final AuditLogRepository auditLog;

	public AuditLogService(AuditLogRepository auditLog) {
		this.auditLog = auditLog;
	}

	/** Must run inside the money-movement transaction: no money moves without its audit row. */
	@Transactional(propagation = Propagation.MANDATORY)
	public void record(User actor, String action, String entityType, Long entityId, String detail) {
		auditLog.save(AuditLog.of(actor, action, entityType, entityId, detail));
	}

}
