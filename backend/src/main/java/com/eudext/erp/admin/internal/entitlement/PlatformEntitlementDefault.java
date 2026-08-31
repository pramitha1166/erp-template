package com.eudext.erp.admin.internal.entitlement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

/** ADM-1 / BRD-12: a platform-wide default a Brand inherits unless it has its own {@link BrandEntitlement} override. */
@Entity
@Table(name = "platform_entitlement_defaults")
public class PlatformEntitlementDefault {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "feature_code", nullable = false, updatable = false)
    private String featureCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PlatformEntitlementDefault() {}

    public static PlatformEntitlementDefault of(String featureCode, boolean enabled) {
        PlatformEntitlementDefault entry = new PlatformEntitlementDefault();
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
