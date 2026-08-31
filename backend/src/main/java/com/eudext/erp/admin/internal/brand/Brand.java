package com.eudext.erp.admin.internal.brand;

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
 * ADM-1 / BRD-1: the reseller/white-label identity that owns a set of
 * Tenants. Full brand identity (logos, colour tokens, custom domain, etc.)
 * is Epic 0.8's scope — this is only the lifecycle record Epic 0.11 needs
 * to exist for a Brand to be created/suspended/reactivated and for Tenants
 * to reference.
 *
 * <p>Deliberately carries no {@code tenant_id} and no RLS: a Brand sits
 * above every Tenant, so treating it as tenant-owned data would be a
 * category error, not an omission — see the {@code admin} module's
 * migration for the full reasoning. Access is enforced purely by the
 * {@code admin:platform:manage} permission check in the service layer.
 */
@Entity
@Table(name = "brands")
@EntityListeners(AuditingEntityListener.class)
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "support_email")
    private String supportEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BrandStatus status = BrandStatus.ACTIVE;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspended_reason")
    private String suspendedReason;

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

    protected Brand() {}

    public static Brand create(String name, String legalName, String supportEmail) {
        Brand brand = new Brand();
        brand.name = name;
        brand.legalName = legalName;
        brand.supportEmail = supportEmail;
        return brand;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public BrandStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == BrandStatus.ACTIVE;
    }

    public void suspend(String reason) {
        this.status = BrandStatus.SUSPENDED;
        this.suspendedAt = Instant.now();
        this.suspendedReason = reason;
    }

    public void reactivate() {
        this.status = BrandStatus.ACTIVE;
        this.suspendedAt = null;
        this.suspendedReason = null;
    }
}
