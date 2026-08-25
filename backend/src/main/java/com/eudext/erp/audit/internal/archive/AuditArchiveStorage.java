package com.eudext.erp.audit.internal.archive;

/**
 * AUD-5: the cold-storage target archived audit batches are written to.
 * Kept as an interface — separate from Hibernate/JDBC — purely so {@link
 * AuditArchiveService}'s batching/watermark logic is unit-testable without
 * a real S3-compatible endpoint. {@link S3AuditArchiveStorage} is the only
 * production implementation, backed by the MinIO instance docker-compose
 * provisions locally (NFR-D1) and real S3 in staging/production.
 */
interface AuditArchiveStorage {

    void put(String objectKey, byte[] content);
}
