package com.eudext.erp.audit.internal.archive;

import com.eudext.erp.audit.internal.log.AuditAction;
import com.eudext.erp.audit.internal.log.AuditLogEntry;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Flat, serialization-only shape for a batch written to cold storage — kept separate from the JPA entity on purpose. */
record AuditArchiveEntryView(
        UUID id,
        String entityType,
        String entityId,
        AuditAction action,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        String actor,
        Instant occurredAt,
        String ipAddress,
        String requestId) {

    static AuditArchiveEntryView from(AuditLogEntry entry) {
        return new AuditArchiveEntryView(
                entry.getId(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getAction(),
                entry.getOldValues(),
                entry.getNewValues(),
                entry.getActor(),
                entry.getOccurredAt(),
                entry.getIpAddress(),
                entry.getRequestId());
    }
}
