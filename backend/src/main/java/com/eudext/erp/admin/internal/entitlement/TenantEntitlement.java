package com.eudext.erp.admin.internal.entitlement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

/**
 * ADM-5: overrides a resolved Brand entitlement for one Tenant, bounded by
 * what the Brand itself is entitled to — the bound is enforced in {@link
 * EntitlementService}, not here. Unlike {@code Brand}/{@code
 * BrandEntitlement}, this table IS tenant-owned data and carries the usual
 * {@code tenant_id} + RLS (see the migration).
 */
@Entity
@Table(name = "tenant_entitlements")
public class TenantEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "feature_code", nullable = false, updatable = false)
    private String featureCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected TenantEntitlement() {}

    public static TenantEntitlement of(UUID tenantId, String featureCode, boolean enabled) {
        TenantEntitlement entry = new TenantEntitlement();
        entry.tenantId = tenantId;
        entry.featureCode = featureCode;
        entry.enabled = enabled;
        return entry;
    }

    public UUID getId() {
        return id;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
