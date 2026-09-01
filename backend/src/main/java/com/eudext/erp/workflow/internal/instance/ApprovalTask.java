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
 * WF-4: one actionable task for one resolved approver of a step occurrence.
 * A role step with three holders creates three {@code PENDING} tasks; the
 * step is satisfied the moment any one of them is {@code APPROVED} — the
 * others are then {@code CANCELLED} by {@code WorkflowEngine}, not left
 * dangling. {@code dueAt} (set from the owning step's
 * {@code escalationHours}) is what the WF-5 escalation sweep polls.
 */
@Entity
@Table(name = "approval_tasks")
public class ApprovalTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "instance_id", nullable = false, updatable = false)
    private UUID instanceId;

    @Column(name = "step_id", nullable = false, updatable = false)
    private UUID stepId;

    @Column(name = "sequence_order", nullable = false, updatable = false)
    private int sequenceOrder;

    @Column(name = "assigned_user_id", nullable = false, updatable = false)
    private UUID assignedUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    @Column(name = "decision_comment")
    private String decisionComment;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ApprovalTask() {}

    public static ApprovalTask create(
            UUID tenantId, UUID instanceId, UUID stepId, int sequenceOrder, UUID assignedUserId, Instant dueAt) {
        ApprovalTask task = new ApprovalTask();
        task.tenantId = tenantId;
        task.instanceId = instanceId;
        task.stepId = stepId;
        task.sequenceOrder = sequenceOrder;
        task.assignedUserId = assignedUserId;
        task.dueAt = dueAt;
        task.createdAt = Instant.now();
        return task;
    }

    public void approve(UUID decidedBy, String comment) {
        requirePending();
        this.status = TaskStatus.APPROVED;
        this.decidedBy = decidedBy;
        this.decisionComment = comment;
        this.decidedAt = Instant.now();
    }

    public void reject(UUID decidedBy, String comment) {
        requirePending();
        this.status = TaskStatus.REJECTED;
        this.decidedBy = decidedBy;
        this.decisionComment = comment;
        this.decidedAt = Instant.now();
    }

    /** A sibling task in the same step was decided first, or the instance was rejected/cancelled — this one is moot. */
    public void cancel() {
        if (status != TaskStatus.PENDING) {
            return;
        }
        this.status = TaskStatus.CANCELLED;
    }

    public void escalate() {
        requirePending();
        this.status = TaskStatus.ESCALATED;
    }

    private void requirePending() {
        if (status != TaskStatus.PENDING) {
            throw new IllegalWorkflowStateException("Approval task " + id + " is no longer pending (status=" + status + ")");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public UUID getStepId() {
        return stepId;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public UUID getAssignedUserId() {
        return assignedUserId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getDecisionComment() {
        return decisionComment;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public long getVersion() {
        return version;
    }
}
