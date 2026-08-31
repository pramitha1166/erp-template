package com.eudext.erp.admin.internal.web;

import com.eudext.erp.admin.internal.brand.Brand;
import com.eudext.erp.admin.internal.brand.BrandService;
import com.eudext.erp.admin.internal.entitlement.EntitlementService;
import com.eudext.erp.admin.internal.support.AdminAccessGuard;
import com.eudext.erp.admin.internal.support.AdminPermissions;
import com.eudext.erp.iam.IdentityProvisioningApi;
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

/** ADM-1 / BRD-12: Platform Admin Console — Brand CRUD and platform-wide default entitlements. */
@RestController
@RequestMapping("/admin/brands")
public class BrandController {

    private final BrandService brandService;
    private final EntitlementService entitlementService;
    private final AdminAccessGuard accessGuard;

    public BrandController(BrandService brandService, EntitlementService entitlementService, AdminAccessGuard accessGuard) {
        this.brandService = brandService;
        this.entitlementService = entitlementService;
        this.accessGuard = accessGuard;
    }

    public record CreateBrandRequest(@NotBlank String name, String legalName, @Email String supportEmail) {}

    public record BrandView(UUID id, String name, String legalName, String status) {}

    public record ProvisionAdminRequest(@Email @NotBlank String email) {}

    public record ProvisionedAdminView(UUID userId, String email, String temporaryPassword) {}

    public record SetEntitlementRequest(boolean enabled) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BrandView create(@Valid @RequestBody CreateBrandRequest request) {
        UUID actor = accessGuard.requirePlatformAdmin(AdminPermissions.PLATFORM_MANAGE);
        Brand brand = brandService.create(request.name(), request.legalName(), request.supportEmail(), actor.toString());
        return toView(brand);
    }

    @GetMapping
    public List<BrandView> list() {
        accessGuard.requirePlatformAdmin(AdminPermissions.PLATFORM_MANAGE);
        return brandService.listAll().stream().map(BrandController::toView).toList();
    }

    @GetMapping("/{brandId}")
    public BrandView get(@PathVariable UUID brandId) {
        accessGuard.requireBrandAccess(brandId, AdminPermissions.BRAND_MANAGE);
        return toView(brandService.get(brandId));
    }

    @PostMapping("/{brandId}/suspend")
    public void suspend(@PathVariable UUID brandId, @RequestBody(required = false) SuspendRequest request) {
        UUID actor = accessGuard.requirePlatformAdmin(AdminPermissions.PLATFORM_MANAGE);
        brandService.suspend(brandId, request == null ? null : request.reason(), actor.toString());
    }

    @PostMapping("/{brandId}/reactivate")
    public void reactivate(@PathVariable UUID brandId) {
        UUID actor = accessGuard.requirePlatformAdmin(AdminPermissions.PLATFORM_MANAGE);
        brandService.reactivate(brandId, actor.toString());
    }

    public record SuspendRequest(String reason) {}

    @PostMapping("/{brandId}/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public ProvisionedAdminView provisionAdmin(@PathVariable UUID brandId, @Valid @RequestBody ProvisionAdminRequest request) {
        UUID actor = accessGuard.requirePlatformAdmin(AdminPermissions.PLATFORM_MANAGE);
        IdentityProvisioningApi.ProvisionedUser user = brandService.provisionBrandAdmin(brandId, request.email(), actor.toString());
        return new ProvisionedAdminView(user.userId(), user.email(), user.temporaryPassword());
    }

    @PutMapping("/{brandId}/entitlements/{featureCode}")
    public void setBrandEntitlement(
            @PathVariable UUID brandId, @PathVariable String featureCode, @RequestBody SetEntitlementRequest request) {
        accessGuard.requirePlatformAdmin(AdminPermissions.PLATFORM_MANAGE);
        entitlementService.setBrandEntitlement(brandId, featureCode, request.enabled());
    }

    private static BrandView toView(Brand brand) {
        return new BrandView(brand.getId(), brand.getName(), brand.getLegalName(), brand.getStatus().name());
    }
}
