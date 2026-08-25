package com.eudext.erp.audit.internal.archive;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * AUD-5: per-tenant bookkeeping for {@link AuditArchiveService} — how far
 * the cold-storage export has progressed. Ordinary mutable row; see the
 * V11 migration comment for why this lives in its own table rather than on
 * {@code audit_log} itself (AUD-3 keeps that table strictly insert-only).
 */
@Entity
@Table(name = "audit_archive_watermark")
public class AuditArchiveWatermark {

    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "archived_through", nullable = false)
    private Instant archivedThrough;

    @Column(name = "last_object_key")
    private String lastObjectKey;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected AuditArchiveWatermark() {}

    static AuditArchiveWatermark initial(UUID tenantId, Instant epoch) {
        AuditArchiveWatermark watermark = new AuditArchiveWatermark();
        watermark.tenantId = tenantId;
        watermark.archivedThrough = epoch;
        watermark.updatedAt = Instant.now();
        return watermark;
    }

    void advanceTo(Instant archivedThrough, String objectKey) {
        this.archivedThrough = archivedThrough;
        this.lastObjectKey = objectKey;
        this.updatedAt = Instant.now();
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public Instant getArchivedThrough() {
        return archivedThrough;
    }

    public String getLastObjectKey() {
        return lastObjectKey;
    }
}
