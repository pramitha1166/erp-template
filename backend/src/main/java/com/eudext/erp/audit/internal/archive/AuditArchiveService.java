package com.eudext.erp.audit.internal.archive;

import com.eudext.erp.audit.internal.log.AuditLogEntry;
import com.eudext.erp.audit.internal.log.AuditLogRepository;
import com.eudext.erp.config.tenancy.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * AUD-5: exports a tenant's audit_log rows older than the configured
 * retention window to cold storage and advances that tenant's watermark.
 * Deliberately not {@code @Transactional} at this level — {@link
 * com.eudext.erp.config.tenancy.TenantAwareDataSource} stamps the Postgres
 * session variable RLS reads at *connection checkout*, which for a
 * {@code @Transactional} method happens before the method body runs; since
 * this method is what sets {@link TenantContext} in the first place, that
 * would be too late for its own transaction. Each repository call below
 * gets its own short transaction instead (Spring Data's usual per-method
 * default), all correctly tenant-scoped because {@link TenantContext}
 * stays set on this thread for the whole call.
 */
@Service
class AuditArchiveService {

    private static final Logger log = LoggerFactory.getLogger(AuditArchiveService.class);

    private final AuditLogRepository auditLogRepository;
    private final AuditArchiveWatermarkRepository watermarkRepository;
    private final AuditArchiveProperties properties;
    private final Optional<AuditArchiveStorage> storage;
    private final ObjectMapper objectMapper;

    AuditArchiveService(
            AuditLogRepository auditLogRepository,
            AuditArchiveWatermarkRepository watermarkRepository,
            AuditArchiveProperties properties,
            Optional<AuditArchiveStorage> storage,
            ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.watermarkRepository = watermarkRepository;
        this.properties = properties;
        this.storage = storage;
        this.objectMapper = objectMapper;
    }

    record ArchiveResult(UUID tenantId, int rowsArchived, String objectKey) {}

    ArchiveResult archiveForTenant(UUID tenantId) {
        if (storage.isEmpty()) {
            log.debug("Audit archival storage not configured (eudext.audit.archive.enabled=false); skipping tenant {}", tenantId);
            return new ArchiveResult(tenantId, 0, null);
        }
        TenantContext.set(tenantId);
        try {
            Instant cutoff = OffsetDateTime.now(ZoneOffset.UTC)
                    .minus(properties.getRetentionBeforeArchive())
                    .toInstant();
            AuditArchiveWatermark watermark =
                    watermarkRepository.findById(tenantId).orElseGet(() -> AuditArchiveWatermark.initial(tenantId, Instant.EPOCH));

            List<AuditLogEntry> batch = auditLogRepository
                    .findByTenantIdAndOccurredAtAfterAndOccurredAtLessThanEqualOrderByOccurredAtAsc(
                            tenantId, watermark.getArchivedThrough(), cutoff);
            if (batch.isEmpty()) {
                return new ArchiveResult(tenantId, 0, null);
            }

            String objectKey = objectKey(tenantId, watermark.getArchivedThrough(), cutoff);
            storage.get().put(objectKey, serialize(batch));

            watermark.advanceTo(batch.get(batch.size() - 1).getOccurredAt(), objectKey);
            watermarkRepository.save(watermark);

            log.info("Archived {} audit_log rows for tenant {} to {}", batch.size(), tenantId, objectKey);
            return new ArchiveResult(tenantId, batch.size(), objectKey);
        } finally {
            TenantContext.clear();
        }
    }

    private byte[] serialize(List<AuditLogEntry> batch) {
        try {
            return objectMapper.writeValueAsBytes(batch.stream().map(AuditArchiveEntryView::from).toList());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize audit archive batch", e);
        }
    }

    private static String objectKey(UUID tenantId, Instant from, Instant through) {
        return "%s/%s_%s.json".formatted(tenantId, from, through);
    }
}
