package com.eudext.erp.masterdata.internal.currency;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MDM-8. {@code tenantIds} is the same stopgap {@code AuditArchiveProperties} documents: no tenant registry to
 * enumerate every tenant from yet (Epic 0.9), so the scheduled import can only run for tenants it's told about.
 */
@ConfigurationProperties(prefix = "eudext.masterdata.currency.cbsl-import")
public class CbslImportProperties {

    /** Master switch — off by default; the bundled {@link NoopCbslRateSource} would make it a no-op anyway. */
    private boolean enabled = false;

    private List<UUID> tenantIds = List.of();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<UUID> getTenantIds() {
        return tenantIds;
    }

    public void setTenantIds(List<UUID> tenantIds) {
        this.tenantIds = tenantIds;
    }
}
