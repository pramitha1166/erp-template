package com.eudext.erp.workflow.internal.delegation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * WF-5: {@code delegateUserId} may act on {@code delegatorUserId}'s
 * assigned approval tasks for the inclusive {@code [startDate, endDate]}
 * range, unless {@code revoked}. This only ever widens who may decide a
 * task already assigned to the delegator — it never reassigns the task's
 * {@code assignedUserId}, so the approval history still shows whose
 * authority a decision was made under.
 */
@Entity
@Table(name = "approval_delegations")
@EntityListeners(AuditingEntityListener.class)
public class ApprovalDelegation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "delegator_user_id", nullable = false, updatable = false)
    private UUID delegatorUserId;

    @Column(name = "delegate_user_id", nullable = false, updatable = false)
    private UUID delegateUserId;

    @Column(name = "start_date", nullable = false, updatable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false, updatable = false)
    private LocalDate endDate;

    @Column(name = "reason")
    private String reason;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

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

    protected ApprovalDelegation() {}

    public static ApprovalDelegation create(
            UUID tenantId, UUID delegatorUserId, UUID delegateUserId, LocalDate startDate, LocalDate endDate, String reason) {
        if (delegatorUserId.equals(delegateUserId)) {
            throw new IllegalArgumentException("A user cannot delegate to themselves");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate cannot be before startDate");
        }
        ApprovalDelegation delegation = new ApprovalDelegation();
        delegation.tenantId = tenantId;
        delegation.delegatorUserId = delegatorUserId;
        delegation.delegateUserId = delegateUserId;
        delegation.startDate = startDate;
        delegation.endDate = endDate;
        delegation.reason = reason;
        return delegation;
    }

    public boolean isActiveOn(LocalDate date) {
        return !revoked && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public void revoke() {
        this.revoked = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getDelegatorUserId() {
        return delegatorUserId;
    }

    public UUID getDelegateUserId() {
        return delegateUserId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getReason() {
        return reason;
    }

    public boolean isRevoked() {
        return revoked;
    }
}
