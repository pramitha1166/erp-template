package com.eudext.erp.iam.internal.sod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
 * IAM-7: a conflicting pair of permission codes that must never both be
 * held by the same user in the same company. {@code permissionCodeA} is
 * always the lexicographically smaller of the two (see
 * {@link SegregationOfDutiesService#normalize}) so the same conflict can't
 * be stored twice in reversed order — matches the V9 migration's check
 * constraint.
 */
@Entity
@Table(name = "sod_rules")
@EntityListeners(AuditingEntityListener.class)
public class SodRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "permission_code_a", nullable = false, updatable = false)
    private String permissionCodeA;

    @Column(name = "permission_code_b", nullable = false, updatable = false)
    private String permissionCodeB;

    @Column(name = "description")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

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

    protected SodRule() {}

    public static SodRule create(UUID tenantId, String permissionCodeA, String permissionCodeB, String description) {
        SodRule rule = new SodRule();
        rule.tenantId = tenantId;
        rule.permissionCodeA = permissionCodeA;
        rule.permissionCodeB = permissionCodeB;
        rule.description = description;
        return rule;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getPermissionCodeA() {
        return permissionCodeA;
    }

    public String getPermissionCodeB() {
        return permissionCodeB;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
