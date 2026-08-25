package com.eudext.erp.iam.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.iam.internal.auth.AccessControlService;
import com.eudext.erp.iam.internal.settings.SecurityPolicy;
import com.eudext.erp.iam.internal.settings.TenantSecuritySettingsService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** IAM-8 / IAM-9: a tenant's session idle timeout and password policy. */
@RestController
@RequestMapping("/iam/security-settings")
public class SecuritySettingsController {

    private static final String PERMISSION_MANAGE_SETTINGS = "iam:security-settings:manage";

    private final TenantSecuritySettingsService settingsService;
    private final AccessControlService accessControlService;

    public SecuritySettingsController(TenantSecuritySettingsService settingsService, AccessControlService accessControlService) {
        this.settingsService = settingsService;
        this.accessControlService = accessControlService;
    }

    @GetMapping
    public SecurityPolicy get() {
        return settingsService.resolve(tenantId());
    }

    @PutMapping
    public SecurityPolicy update(@RequestParam UUID companyId, @RequestBody SecurityPolicy policy) {
        accessControlService.requirePermission(companyId, PERMISSION_MANAGE_SETTINGS);
        return settingsService.update(tenantId(), policy);
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
