package com.eudext.erp.audit.internal.log;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * AUD-3: deliberately extends the bare Spring Data {@link Repository}
 * marker rather than {@code JpaRepository} or {@code CrudRepository} — it
 * exposes only the read methods declared below, none of which can mutate
 * or delete a row. There is no {@code save}, {@code delete}, or
 * {@code deleteById} anywhere in this interface, and none should ever be
 * added; the sole write path for audit_log is {@code AuditLogWriter}'s
 * JDBC INSERT.
 */
public interface AuditLogRepository extends Repository<AuditLogEntry, UUID> {

    List<AuditLogEntry> findByTenantIdAndEntityTypeAndEntityIdOrderByOccurredAtAsc(
            UUID tenantId, String entityType, String entityId);

    /** AUD-5: the archival window is exclusive of {@code after} (already-archived) and inclusive of {@code through}. */
    List<AuditLogEntry> findByTenantIdAndOccurredAtAfterAndOccurredAtLessThanEqualOrderByOccurredAtAsc(
            UUID tenantId, Instant after, Instant through);
}
