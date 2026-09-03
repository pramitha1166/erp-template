package com.eudext.erp.documents.internal.attachment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * DOC-1: a file attached to a record owned by another module. {@code documentType} + {@code documentId} is a
 * generic, non-FK reference — the documents module never reaches into another module's tables (ARCH-1) — where
 * {@code documentType} is a {@code module:entity} pair (e.g. {@code sales:invoice}) used verbatim by DOC-5's
 * permission check.
 */
@Entity
@Table(name = "attachments")
@EntityListeners(AuditingEntityListener.class)
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "document_type", nullable = false, updatable = false)
    private String documentType;

    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    @Column(name = "file_name", nullable = false, updatable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false, updatable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, updatable = false)
    private String storageKey;

    @Column(name = "checksum_sha256", nullable = false, updatable = false)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status", nullable = false)
    private ScanStatus scanStatus;

    @Column(name = "scan_message")
    private String scanMessage;

    @CreatedBy
    @Column(name = "uploaded_by", updatable = false)
    private String uploadedBy;

    @CreatedDate
    @Column(name = "uploaded_at", updatable = false)
    private Instant uploadedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Attachment() {}

    public static Attachment create(
            UUID tenantId,
            UUID companyId,
            String documentType,
            UUID documentId,
            String fileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            String checksumSha256,
            ScanStatus scanStatus,
            String scanMessage) {
        Attachment attachment = new Attachment();
        attachment.tenantId = tenantId;
        attachment.companyId = companyId;
        attachment.documentType = documentType;
        attachment.documentId = documentId;
        attachment.fileName = fileName;
        attachment.contentType = contentType;
        attachment.sizeBytes = sizeBytes;
        attachment.storageKey = storageKey;
        attachment.checksumSha256 = checksumSha256;
        attachment.scanStatus = scanStatus;
        attachment.scanMessage = scanMessage;
        return attachment;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public ScanStatus getScanStatus() {
        return scanStatus;
    }

    public String getScanMessage() {
        return scanMessage;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public long getVersion() {
        return version;
    }
}
