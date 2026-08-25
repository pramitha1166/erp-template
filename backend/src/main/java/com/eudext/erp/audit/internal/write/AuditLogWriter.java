package com.eudext.erp.audit.internal.write;

import com.eudext.erp.audit.internal.log.AuditAction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * AUD-1 / AUD-3: the one and only place that writes an audit_log row.
 * Deliberately plain JDBC rather than the JPA/Hibernate {@code EntityManager}:
 *
 * <ul>
 *   <li>{@code AuditingInterceptor} is itself a Hibernate {@code Interceptor}
 *       wired into the entity manager factory via {@code
 *       hibernate.session_factory.interceptor} — that bean has to exist
 *       *before* the factory it's plugged into, so it cannot depend on a
 *       JPA repository (which needs that same factory) without a circular
 *       bean-creation error.
 *   <li>Calling back into the same Hibernate session mid-flush (which is
 *       when the interceptor fires) to persist another entity is a classic
 *       reentrant-flush hazard. Writing through a completely separate JDBC
 *       path sidesteps it entirely.
 * </ul>
 *
 * <p>Also used directly by {@code AuthAuditEventListener} for IAM-10's auth
 * events, so there is exactly one write path for every audit_log row
 * regardless of where it originates.
 */
@Component
public class AuditLogWriter {

    private static final Logger log = LoggerFactory.getLogger(AuditLogWriter.class);

    private static final String INSERT_SQL =
            "INSERT INTO audit_log "
                    + "(id, tenant_id, entity_type, entity_id, action, changes, actor, occurred_at, ip_address, request_id) "
                    + "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AuditLogWriter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void write(
            UUID tenantId,
            String entityType,
            String entityId,
            AuditAction action,
            Map<String, Object> oldValues,
            Map<String, Object> newValues,
            String actor,
            String ipAddress,
            String requestId,
            Instant occurredAt) {
        if (tenantId == null) {
            log.warn("Dropping audit entry for {}/{}: no tenant context available", entityType, entityId);
            return;
        }
        String changesJson = toJson(Map.of("old", oldValues, "new", newValues));
        jdbcTemplate.update(
                INSERT_SQL,
                UUID.randomUUID(),
                tenantId,
                entityType,
                entityId,
                action.name(),
                changesJson,
                actor == null ? "system" : actor,
                occurredAt,
                ipAddress,
                requestId);
    }

    void write(PendingAuditEntry entry) {
        write(
                entry.tenantId(),
                entry.entityType(),
                entry.entityId(),
                entry.action(),
                entry.oldValues(),
                entry.newValues(),
                entry.actor(),
                entry.ipAddress(),
                entry.requestId(),
                entry.occurredAt());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize audit changes to JSON", e);
        }
    }
}
