package com.wallet.api.audit;

import java.time.Instant;

import com.wallet.api.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One immutable "who did what" record. Database triggers reject any UPDATE, DELETE or
 * TRUNCATE, so the history cannot be rewritten even by a future careless query.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "actor_user_id", nullable = false)
	private User actor;

	@Column(nullable = false, length = 50)
	private String action;

	@Column(name = "entity_type", nullable = false, length = 50)
	private String entityType;

	@Column(name = "entity_id")
	private Long entityId;

	@Column(length = 500)
	private String detail;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AuditLog() {
		// for JPA
	}

	private AuditLog(User actor, String action, String entityType, Long entityId, String detail) {
		this.actor = actor;
		this.action = action;
		this.entityType = entityType;
		this.entityId = entityId;
		this.detail = detail;
		this.createdAt = Instant.now();
	}

	public static AuditLog of(User actor, String action, String entityType, Long entityId, String detail) {
		return new AuditLog(actor, action, entityType, entityId, detail);
	}

	public Long getId() {
		return id;
	}

	public User getActor() {
		return actor;
	}

	public String getAction() {
		return action;
	}

	public String getEntityType() {
		return entityType;
	}

	public Long getEntityId() {
		return entityId;
	}

	public String getDetail() {
		return detail;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
