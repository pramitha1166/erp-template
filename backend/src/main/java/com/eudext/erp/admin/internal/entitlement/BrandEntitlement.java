package com.eudext.erp.admin.internal.entitlement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

/** BRD-12: overrides a {@link PlatformEntitlementDefault} for one Brand. No RLS — see {@code Brand}'s own javadoc. */
@Entity
@Table(name = "brand_entitlements")
public class BrandEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "brand_id", nullable = false, updatable = false)
    private UUID brandId;

    @Column(name = "feature_code", nullable = false, updatable = false)
    private String featureCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected BrandEntitlement() {}

    public static BrandEntitlement of(UUID brandId, String featureCode, boolean enabled) {
        BrandEntitlement entry = new BrandEntitlement();
        entry.brandId = brandId;
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
