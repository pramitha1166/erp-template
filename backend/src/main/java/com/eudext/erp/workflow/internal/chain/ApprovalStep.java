package com.eudext.erp.workflow.internal.chain;

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
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * WF-3 / WF-4: one step of a chain. Steps sharing the same
 * {@code sequenceOrder} within a chain form a parallel group — every step
 * in the group must be satisfied before an instance advances to the next
 * (higher) sequence order; steps at different orders run sequentially.
 *
 * <p>{@code approverType} selects which of {@code approverRoleId} /
 * {@code approverUserId} / {@code hierarchyLevel} is meaningful (WF-3).
 * {@code escalationHours} is optional (WF-5): when set, a task created for
 * this step becomes due that many hours after creation, and the escalation
 * sweep resolves {@code escalationType}/{@code escalationRoleId}/
 * {@code escalationUserId}/{@code escalationHierarchyLevel} the same way —
 * falling back to the assigned approver's own manager if no escalation
 * target is configured at all.
 */
@Entity
@Table(name = "approval_steps")
@EntityListeners(AuditingEntityListener.class)
public class ApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "chain_id", nullable = false, updatable = false)
    private UUID chainId;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_type", nullable = false)
    private ApproverType approverType;

    @Column(name = "approver_role_id")
    private UUID approverRoleId;

    @Column(name = "approver_user_id")
    private UUID approverUserId;

    @Column(name = "hierarchy_level")
    private Integer hierarchyLevel;

    @Column(name = "escalation_hours")
    private Integer escalationHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_type")
    private ApproverType escalationType;

    @Column(name = "escalation_role_id")
    private UUID escalationRoleId;

    @Column(name = "escalation_user_id")
    private UUID escalationUserId;

    @Column(name = "escalation_hierarchy_level")
    private Integer escalationHierarchyLevel;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedBy
    @Column(name = "modified_by")
    private String modifiedBy;

    @LastModifiedDate
    @Column(name = "modified_at")
    private Instant modifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ApprovalStep() {}

    public static ApprovalStep role(UUID tenantId, UUID chainId, int sequenceOrder, String name, UUID roleId) {
        ApprovalStep step = base(tenantId, chainId, sequenceOrder, name, ApproverType.ROLE);
        step.approverRoleId = roleId;
        return step;
    }

    public static ApprovalStep user(UUID tenantId, UUID chainId, int sequenceOrder, String name, UUID userId) {
        ApprovalStep step = base(tenantId, chainId, sequenceOrder, name, ApproverType.USER);
        step.approverUserId = userId;
        return step;
    }

    public static ApprovalStep hierarchy(UUID tenantId, UUID chainId, int sequenceOrder, String name, int level) {
        ApprovalStep step = base(tenantId, chainId, sequenceOrder, name, ApproverType.HIERARCHY);
        step.hierarchyLevel = level;
        return step;
    }

    private static ApprovalStep base(UUID tenantId, UUID chainId, int sequenceOrder, String name, ApproverType type) {
        ApprovalStep step = new ApprovalStep();
        step.tenantId = tenantId;
        step.chainId = chainId;
        step.sequenceOrder = sequenceOrder;
        step.name = name;
        step.approverType = type;
        return step;
    }

    public void configureEscalation(Integer hours, ApproverType type, UUID roleId, UUID userId, Integer level) {
        this.escalationHours = hours;
        this.escalationType = type;
        this.escalationRoleId = roleId;
        this.escalationUserId = userId;
        this.escalationHierarchyLevel = level;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getChainId() {
        return chainId;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public String getName() {
        return name;
    }

    public ApproverType getApproverType() {
        return approverType;
    }

    public UUID getApproverRoleId() {
        return approverRoleId;
    }

    public UUID getApproverUserId() {
        return approverUserId;
    }

    public Integer getHierarchyLevel() {
        return hierarchyLevel;
    }

    public Integer getEscalationHours() {
        return escalationHours;
    }

    public ApproverType getEscalationType() {
        return escalationType;
    }

    public UUID getEscalationRoleId() {
        return escalationRoleId;
    }

    public UUID getEscalationUserId() {
        return escalationUserId;
    }

    public Integer getEscalationHierarchyLevel() {
        return escalationHierarchyLevel;
    }

    public long getVersion() {
        return version;
    }
}
