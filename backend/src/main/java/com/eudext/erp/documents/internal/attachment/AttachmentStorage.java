package com.eudext.erp.documents.internal.attachment;

/**
 * DOC-1: the object-storage target attachment bytes are written to. Kept as an interface — separate from the AWS SDK
 * — purely so {@link AttachmentService} is unit-testable without a reachable S3-compatible endpoint, same pattern as
 * {@code com.eudext.erp.audit.internal.archive.AuditArchiveStorage}. {@link S3AttachmentStorage} is the only
 * production implementation, backed by the MinIO instance docker-compose provisions locally (NFR-D1) and real S3 in
 * staging/production.
 */
interface AttachmentStorage {

    void put(String objectKey, byte[] content, String contentType);

    byte[] get(String objectKey);

    void delete(String objectKey);
}
