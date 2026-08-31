package com.eudext.erp.admin.internal.datarequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * ADM-8 / NFR-S7 / NFR-D5: a tracked data export or erasure request.
 * Tenant-owned; RLS applies. {@code resultPayload} holds the export bundle
 * inline as JSON — Phase 0 has no bulk transactional data yet (no
 * documents, no attachments), so a full async S3 export pipeline (Epic
 * 0.10.3's own scope once it exists) would be premature; this is
 * everything the platform currently holds about a tenant.
 */
@Entity
@Table(name = "data_subject_requests")
public class DataSubjectRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false)
    private DataRequestType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DataRequestStatus status = DataRequestStatus.PENDING;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private String requestedBy;

    @Column(name = "notes")
    private String notes;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "result_payload", columnDefinition = "jsonb")
    private String resultPayloadJson;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected DataSubjectRequest() {}

    public static DataSubjectRequest create(UUID tenantId, DataRequestType type, String requestedBy, String notes) {
        DataSubjectRequest request = new DataSubjectRequest();
        request.tenantId = tenantId;
        request.type = type;
        request.requestedBy = requestedBy;
        request.notes = notes;
        request.requestedAt = Instant.now();
        return request;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public DataRequestType getType() {
        return type;
    }

    public DataRequestStatus getStatus() {
        return status;
    }

    public String getResultPayloadJson() {
        return resultPayloadJson;
    }

    public void complete(String resultPayloadJson) {
        this.status = DataRequestStatus.COMPLETED;
        this.resultPayloadJson = resultPayloadJson;
        this.completedAt = Instant.now();
    }

    public void fail(String reason) {
        this.status = DataRequestStatus.FAILED;
        this.resultPayloadJson = reason;
        this.completedAt = Instant.now();
    }
}
