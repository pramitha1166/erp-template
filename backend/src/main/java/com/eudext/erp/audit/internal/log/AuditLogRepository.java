package com.eudext.erp.audit.internal.log;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

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

    /**
     * AUD-2 / F0.3.2: filtered, paginated browse across every entity for a
     * tenant — each {@code :param IS NULL OR ...} clause makes that filter
     * optional so the admin screen can search on any subset of entity type,
     * actor, action, and date range.
     */
    @Query("""
            SELECT e FROM AuditLogEntry e
            WHERE e.tenantId = :tenantId
              AND (:entityType IS NULL OR e.entityType = :entityType)
              AND (:actor IS NULL OR e.actor = :actor)
              AND (:action IS NULL OR e.action = :action)
              AND (:from IS NULL OR e.occurredAt >= :from)
              AND (:through IS NULL OR e.occurredAt <= :through)
            ORDER BY e.occurredAt DESC
            """)
    Page<AuditLogEntry> search(
            @Param("tenantId") UUID tenantId,
            @Param("entityType") String entityType,
            @Param("actor") String actor,
            @Param("action") AuditAction action,
            @Param("from") Instant from,
            @Param("through") Instant through,
            Pageable pageable);
}
