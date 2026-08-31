package com.eudext.erp.admin.internal.web;

import com.eudext.erp.admin.internal.entitlement.EntitlementService;
import com.eudext.erp.admin.internal.support.AdminAccessGuard;
import com.eudext.erp.admin.internal.support.AdminPermissions;
import com.eudext.erp.admin.internal.support.PlatformBootstrapService;
import com.eudext.erp.admin.internal.usage.PlatformUsageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** ADM-1 / ADM-9: platform-wide bootstrap, default entitlements, and usage/health rollup. */
@RestController
@RequestMapping("/admin/platform")
public class PlatformController {

    private final PlatformBootstrapService bootstrapService;
    private final EntitlementService entitlementService;
    private final PlatformUsageService usageService;
    private final AdminAccessGuard accessGuard;

    public PlatformController(
            PlatformBootstrapService bootstrapService,
            EntitlementService entitlementService,
            PlatformUsageService usageService,
            AdminAccessGuard accessGuard) {
        this.bootstrapService = bootstrapService;
        this.entitlementService = entitlementService;
        this.usageService = usageService;
        this.accessGuard = accessGuard;
    }

    public record BootstrapRequest(@Email @NotBlank String email) {}

    public record ProvisionedAdminView(UUID userId, String email, String temporaryPassword) {}

    public record SetEntitlementRequest(boolean enabled) {}

    public record EntitlementView(String featureCode, boolean enabled) {}

    /** ADM-1: unauthenticated by design — see {@link PlatformBootstrapService}'s javadoc for why this is safe. */
    @PostMapping("/bootstrap")
    @ResponseStatus(HttpStatus.CREATED)
    public ProvisionedAdminView bootstrap(@Valid @RequestBody BootstrapRequest request) {
        var user = bootstrapService.bootstrapFirstPlatformAdmin(request.email());
        return new ProvisionedAdminView(user.userId(), user.email(), user.temporaryPassword());
    }

    @PutMapping("/entitlements/{featureCode}")
    public void setDefault(@PathVariable String featureCode, @RequestBody SetEntitlementRequest request) {
        accessGuard.requirePlatformAdmin(AdminPermissions.PLATFORM_MANAGE);
        entitlementService.setPlatformDefault(featureCode, request.enabled());
    }

    @GetMapping("/entitlements")
    public List<EntitlementView> listDefaults() {
        accessGuard.requirePlatformAdmin(AdminPermissions.PLATFORM_MANAGE);
        return entitlementService.listPlatformDefaults().stream()
                .map(entry -> new EntitlementView(entry.getFeatureCode(), entry.isEnabled()))
                .toList();
    }

    @GetMapping("/usage")
    public PlatformUsageService.PlatformUsage usage() {
        accessGuard.requirePlatformAdmin(AdminPermissions.USAGE_READ);
        return usageService.summarize();
    }
}
