package com.eudext.erp.audit.internal.archive;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** AUD-5: read side backing the admin-only retention/archival status indicator (F0.3.3). */
@Service
public class AuditArchiveStatusService {

    /** SRS AUD-5: a fixed compliance floor, not a per-tenant setting — audit rows are never deleted before this. */
    private static final int MINIMUM_RETENTION_YEARS = 7;

    private final AuditArchiveWatermarkRepository watermarkRepository;
    private final AuditArchiveProperties properties;

    public AuditArchiveStatusService(AuditArchiveWatermarkRepository watermarkRepository, AuditArchiveProperties properties) {
        this.watermarkRepository = watermarkRepository;
        this.properties = properties;
    }

    public record ArchiveStatus(
            boolean archivalEnabled,
            Instant archivedThrough,
            String lastObjectKey,
            int coldStorageAfterYears,
            int minimumRetentionYears) {}

    @Transactional(readOnly = true)
    public ArchiveStatus statusFor(UUID tenantId) {
        return watermarkRepository
                .findById(tenantId)
                .map(watermark -> new ArchiveStatus(
                        properties.isEnabled(),
                        watermark.getArchivedThrough(),
                        watermark.getLastObjectKey(),
                        coldStorageAfterYears(),
                        MINIMUM_RETENTION_YEARS))
                .orElseGet(() -> new ArchiveStatus(properties.isEnabled(), null, null, coldStorageAfterYears(), MINIMUM_RETENTION_YEARS));
    }

    private int coldStorageAfterYears() {
        return properties.getRetentionBeforeArchive().getYears();
    }
}
