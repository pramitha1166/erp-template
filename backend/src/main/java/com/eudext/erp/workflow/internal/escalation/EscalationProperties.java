package com.eudext.erp.workflow.internal.escalation;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WF-5. {@code tenantIds} is the same stopgap {@code AuditArchiveProperties}
 * documents (AUD-5): there is no tenant registry to enumerate from yet
 * (Phase 0), so the escalation sweep can only scan tenants it's explicitly
 * told about. Epic 0.9's tenant registry replaces this with real
 * enumeration.
 */
@ConfigurationProperties(prefix = "eudext.workflow.escalation")
public class EscalationProperties {

    /** Master switch — off by default so no environment runs the sweep without configuring tenantIds first. */
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
