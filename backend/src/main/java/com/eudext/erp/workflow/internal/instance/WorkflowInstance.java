package com.eudext.erp.workflow.internal.instance;

import com.eudext.erp.workflow.IllegalWorkflowStateException;
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
 * A running (or resolved) approval process for one document. At most one
 * {@code PENDING} instance may exist per {@code (documentType, documentId)}
 * at a time (enforced by the V21 partial unique index); the document-owning
 * module is expected to hold {@code Document.submit()} until this instance
 * reaches {@code APPROVED} — see {@code WorkflowApi}'s javadoc for why WF-6
 * ("rejection returns to draft") is satisfied by never having left draft in
 * the first place, rather than by an in-place status reversal.
 */
@Entity
@Table(name = "workflow_instances")
public class WorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "branch_id", updatable = false)
    private UUID branchId;

    @Column(name = "document_type", nullable = false, updatable = false)
    private String documentType;

    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    @Column(name = "chain_id", nullable = false, updatable = false)
    private UUID chainId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InstanceStatus status = InstanceStatus.PENDING;

    @Column(name = "current_sequence_order")
    private Integer currentSequenceOrder;

    @Column(name = "submitted_by", nullable = false, updatable = false)
    private UUID submittedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "modified_at")
    private Instant modifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected WorkflowInstance() {}

    public static WorkflowInstance start(
            UUID tenantId, UUID companyId, UUID branchId, String documentType, UUID documentId, UUID chainId, UUID submittedBy) {
        WorkflowInstance instance = new WorkflowInstance();
        instance.tenantId = tenantId;
        instance.companyId = companyId;
        instance.branchId = branchId;
        instance.documentType = documentType;
        instance.documentId = documentId;
        instance.chainId = chainId;
        instance.submittedBy = submittedBy;
        instance.createdAt = Instant.now();
        return instance;
    }

    public void advanceTo(int sequenceOrder) {
        requirePending();
        this.currentSequenceOrder = sequenceOrder;
        this.modifiedAt = Instant.now();
    }

    public void approve() {
        requirePending();
        this.status = InstanceStatus.APPROVED;
        this.modifiedAt = Instant.now();
    }

    public void reject() {
        requirePending();
        this.status = InstanceStatus.REJECTED;
        this.modifiedAt = Instant.now();
    }

    public void cancel() {
        requirePending();
        this.status = InstanceStatus.CANCELLED;
        this.modifiedAt = Instant.now();
    }

    private void requirePending() {
        if (status != InstanceStatus.PENDING) {
            throw new IllegalWorkflowStateException("Workflow instance " + id + " is no longer pending (status=" + status + ")");
        }
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

    public UUID getBranchId() {
        return branchId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public UUID getChainId() {
        return chainId;
    }

    public InstanceStatus getStatus() {
        return status;
    }

    public Integer getCurrentSequenceOrder() {
        return currentSequenceOrder;
    }

    public UUID getSubmittedBy() {
        return submittedBy;
    }

    public long getVersion() {
        return version;
    }
}
