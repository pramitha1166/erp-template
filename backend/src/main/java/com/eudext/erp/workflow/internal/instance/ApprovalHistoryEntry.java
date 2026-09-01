package com.eudext.erp.workflow.internal.instance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * WF-7: one append-only row in a document's approval history (timestamps
 * and comments). Immutable at the database level too — see the
 * {@code approval_history_no_update}/{@code _no_delete} triggers in V21 —
 * and {@link ApprovalHistoryRepository} never declares an update or delete
 * method, mirroring how {@code audit_log} (AUD-3) is protected.
 */
@Entity
@Table(name = "approval_history")
public class ApprovalHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "instance_id", nullable = false, updatable = false)
    private UUID instanceId;

    @Column(name = "task_id", updatable = false)
    private UUID taskId;

    @Column(name = "action", nullable = false, updatable = false)
    private String action;

    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Column(name = "comment", updatable = false)
    private String comment;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected ApprovalHistoryEntry() {}

    public static ApprovalHistoryEntry of(
            UUID tenantId, UUID instanceId, UUID taskId, String action, UUID actorUserId, String comment) {
        ApprovalHistoryEntry entry = new ApprovalHistoryEntry();
        entry.tenantId = tenantId;
        entry.instanceId = instanceId;
        entry.taskId = taskId;
        entry.action = action;
        entry.actorUserId = actorUserId;
        entry.comment = comment;
        entry.occurredAt = Instant.now();
        return entry;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public String getAction() {
        return action;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getComment() {
        return comment;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
