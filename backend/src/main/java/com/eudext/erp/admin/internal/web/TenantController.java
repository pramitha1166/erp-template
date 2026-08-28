package com.eudext.erp.admin.internal.web;

import com.eudext.erp.admin.internal.entitlement.EntitlementService;
import com.eudext.erp.admin.internal.support.AdminAccessGuard;
import com.eudext.erp.admin.internal.support.AdminPermissions;
import com.eudext.erp.admin.internal.tenant.Tenant;
import com.eudext.erp.admin.internal.tenant.TenantOnboardingService;
import com.eudext.erp.admin.internal.tenant.TenantService;
import com.eudext.erp.admin.internal.usage.PlatformUsageService;
import com.eudext.erp.masterdata.MasterDataProvisioningApi;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Set;
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

/**
 * ADM-2 / ADM-5 / ADM-6 / ADM-9: tenant onboarding, cross-brand/per-brand
 * reads, and suspension/reactivation. Nested under {@code /admin/brands/
 * {brandId}} for brand-scoped operations (matching ADM-5's "own brand
 * only" boundary — enforced by {@link AdminAccessGuard#requireBrandAccess}),
 * and {@code /admin/tenants} for cross-brand platform reads (ADM-1).
 */
@RestController
public class TenantController {

    private final TenantOnboardingService onboardingService;
    private final TenantService tenantService;
    private final EntitlementService entitlementService;
    private final PlatformUsageService usageService;
    private final AdminAccessGuard accessGuard;

    public TenantController(
            TenantOnboardingService onboardingService,
            TenantService tenantService,
            EntitlementService entitlementService,
            PlatformUsageService usageService,
            AdminAccessGuard accessGuard) {
        this.onboardingService = onboardingService;
        this.tenantService = tenantService;
        this.entitlementService = entitlementService;
        this.usageService = usageService;
        this.accessGuard = accessGuard;
    }

    public record NewCompanyRequest(
            @NotBlank String legalName,
            String registrationNo,
            String vatNo,
            String address,
            @NotBlank String baseCurrency,
            @Min(1) @Max(12) int fiscalYearStartMonth) {}

    public record OnboardTenantRequest(
            @NotBlank String tenantName,
            @Valid NewCompanyRequest company,
            @Email @NotBlank String adminEmail,
            Set<String> initialEntitlementFeatureCodes) {}

    public record TenantView(UUID id, UUID brandId, String name, String status, UUID primaryCompanyId) {}

    public record SuspendRequest(String reason) {}

    public record SetEntitlementRequest(boolean enabled) {}

    @PostMapping("/admin/brands/{brandId}/tenants")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantView onboard(@PathVariable UUID brandId, @Valid @RequestBody OnboardTenantRequest request) {
        UUID actor = accessGuard.requireBrandAccess(brandId, AdminPermissions.TENANT_ONBOARD);
        Set<String> entitlements = request.initialEntitlementFeatureCodes() == null ? Set.of() : request.initialEntitlementFeatureCodes();
        var company = new MasterDataProvisioningApi.NewCompany(
                request.company().legalName(),
                request.company().registrationNo(),
                request.company().vatNo(),
                request.company().address(),
                request.company().baseCurrency(),
                request.company().fiscalYearStartMonth());
        Tenant tenant = onboardingService.onboard(
                brandId,
                new TenantOnboardingService.Request(request.tenantName(), company, request.adminEmail(), entitlements),
                actor.toString());
        return toView(tenant);
    }

    @GetMapping("/admin/brands/{brandId}/tenants")
    public List<TenantView> listByBrand(@PathVariable UUID brandId) {
        accessGuard.requireBrandAccess(brandId, AdminPermissions.TENANT_MANAGE);
        return tenantService.listByBrand(brandId).stream().map(TenantController::toView).toList();
    }

    @GetMapping("/admin/tenants")
    public List<TenantView> listAll() {
        accessGuard.requirePlatformAdmin(AdminPermissions.PLATFORM_MANAGE);
        return tenantService.listAll().stream().map(TenantController::toView).toList();
    }

    @GetMapping("/admin/brands/{brandId}/tenants/{tenantId}/usage")
    public PlatformUsageService.TenantUsage tenantUsage(@PathVariable UUID brandId, @PathVariable UUID tenantId) {
        accessGuard.requireBrandAccess(brandId, AdminPermissions.USAGE_READ);
        tenantService.requireOwnedByBrand(tenantId, brandId);
        return usageService.tenantUsage(tenantId);
    }

    @PostMapping("/admin/brands/{brandId}/tenants/{tenantId}/suspend")
    public void suspend(@PathVariable UUID brandId, @PathVariable UUID tenantId, @RequestBody(required = false) SuspendRequest request) {
        UUID actor = accessGuard.requireBrandAccess(brandId, AdminPermissions.TENANT_MANAGE);
        tenantService.requireOwnedByBrand(tenantId, brandId);
        tenantService.suspend(tenantId, request == null ? null : request.reason(), actor.toString());
    }

    @PostMapping("/admin/brands/{brandId}/tenants/{tenantId}/reactivate")
    public void reactivate(@PathVariable UUID brandId, @PathVariable UUID tenantId) {
        UUID actor = accessGuard.requireBrandAccess(brandId, AdminPermissions.TENANT_MANAGE);
        tenantService.requireOwnedByBrand(tenantId, brandId);
        tenantService.reactivate(tenantId, actor.toString());
    }

    @PutMapping("/admin/brands/{brandId}/tenants/{tenantId}/entitlements/{featureCode}")
    public void setTenantEntitlement(
            @PathVariable UUID brandId,
            @PathVariable UUID tenantId,
            @PathVariable String featureCode,
            @RequestBody SetEntitlementRequest request) {
        accessGuard.requireBrandAccess(brandId, AdminPermissions.TENANT_MANAGE);
        tenantService.requireOwnedByBrand(tenantId, brandId);
        entitlementService.setTenantEntitlement(brandId, tenantId, featureCode, request.enabled());
    }

    private static TenantView toView(Tenant tenant) {
        return new TenantView(tenant.getId(), tenant.getBrandId(), tenant.getName(), tenant.getStatus().name(), tenant.getPrimaryCompanyId());
    }
}
