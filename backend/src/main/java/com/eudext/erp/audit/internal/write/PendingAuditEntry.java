package com.eudext.erp.audit.internal.write;

import com.eudext.erp.audit.internal.log.AuditAction;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A captured mutation waiting to be persisted once its transaction commits.
 * Actor/IP/request id are snapshotted eagerly at capture time (mid-flush,
 * where {@code SecurityContextHolder} and {@code RequestContextHolder} are
 * reliably populated) rather than re-read later from the post-commit
 * {@code TransactionSynchronization} callback that flushes these — safer
 * than assuming that callback still runs on a thread with the same context.
 */
record PendingAuditEntry(
        UUID tenantId,
        String entityType,
        String entityId,
        AuditAction action,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        String actor,
        String ipAddress,
        String requestId,
        Instant occurredAt) {}
