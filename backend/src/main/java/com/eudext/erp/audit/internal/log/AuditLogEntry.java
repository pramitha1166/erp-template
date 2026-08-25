package com.eudext.erp.audit.internal.log;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * AUD-1 / AUD-2: one row per captured mutation. Read-only from JPA's point
 * of view — {@link Immutable} stops Hibernate from ever issuing an UPDATE
 * for a managed instance of this entity, on top of the database-level
 * enforcement in the V10 migration (AUD-3). The only write path is
 * {@code AuditLogWriter}'s plain JDBC INSERT, which deliberately bypasses
 * JPA entirely (see its javadoc for why).
 */
@Entity
@Table(name = "audit_log")
@Immutable
public class AuditLogEntry {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditAction action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> changes;

    @Column(name = "actor", nullable = false)
    private String actor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "request_id")
    private String requestId;

    protected AuditLogEntry() {}

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public AuditAction getAction() {
        return action;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getOldValues() {
        Object old = changes.get("old");
        return old == null ? Map.of() : (Map<String, Object>) old;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getNewValues() {
        Object updated = changes.get("new");
        return updated == null ? Map.of() : (Map<String, Object>) updated;
    }

    public String getActor() {
        return actor;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getRequestId() {
        return requestId;
    }
}
