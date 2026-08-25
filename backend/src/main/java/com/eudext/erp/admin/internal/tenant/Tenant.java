package com.eudext.erp.admin.internal.tenant;

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
 * ADM-2: the tenant registry itself — the SRS gap this epic exists to
 * close (see the {@code admin} package javadoc). This row's own {@code id}
 * IS the {@code tenant_id} every other tenant-scoped table in the system
 * carries; a table representing the registry of tenants cannot itself be
 * scoped by "which tenant" in the RLS sense, so — like {@link
 * com.eudext.erp.admin.internal.brand.Brand} — it carries no {@code
 * tenant_id} column and no RLS. Access is enforced by the {@code
 * admin:platform:manage} / {@code admin:brand:manage} permission checks in
 * the service layer, and a brand admin's queries are additionally filtered
 * to {@code brandId == their own brand} in Java (see {@code TenantService}).
 */
@Entity
@Table(name = "tenants")
@EntityListeners(AuditingEntityListener.class)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "brand_id", nullable = false, updatable = false)
    private UUID brandId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspended_reason")
    private String suspendedReason;

    /** The tenant-admin user created at onboarding — the default impersonation target (ADM-7) and invite-replacement anchor (ADM-5). */
    @Column(name = "primary_admin_user_id")
    private UUID primaryAdminUserId;

    @Column(name = "primary_company_id")
    private UUID primaryCompanyId;

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

    protected Tenant() {}

    public static Tenant create(UUID brandId, String name) {
        Tenant tenant = new Tenant();
        tenant.brandId = brandId;
        tenant.name = name;
        return tenant;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBrandId() {
        return brandId;
    }

    public String getName() {
        return name;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }

    public UUID getPrimaryAdminUserId() {
        return primaryAdminUserId;
    }

    public UUID getPrimaryCompanyId() {
        return primaryCompanyId;
    }

    public void assignPrimaryAdmin(UUID userId) {
        this.primaryAdminUserId = userId;
    }

    public void assignPrimaryCompany(UUID companyId) {
        this.primaryCompanyId = companyId;
    }

    public void suspend(String reason) {
        this.status = TenantStatus.SUSPENDED;
        this.suspendedAt = Instant.now();
        this.suspendedReason = reason;
    }

    public void reactivate() {
        this.status = TenantStatus.ACTIVE;
        this.suspendedAt = null;
        this.suspendedReason = null;
    }
}
