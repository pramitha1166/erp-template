package com.eudext.erp.admin.internal.usage;

import com.eudext.erp.admin.internal.brand.Brand;
import com.eudext.erp.admin.internal.brand.BrandService;
import com.eudext.erp.admin.internal.tenant.Tenant;
import com.eudext.erp.admin.internal.tenant.TenantService;
import com.eudext.erp.config.tenancy.TenantContextScope;
import com.eudext.erp.iam.IdentityProvisioningApi;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADM-9: platform-wide usage/health rollup, grouped by brand. Storage
 * consumption and transaction volume are reported as zero/not-yet-tracked
 * — Phase 0 has no attachment storage (Epic 0.7) or transactional document
 * volume (Phase 1) to measure yet; reporting an honest placeholder beats
 * fabricating a number.
 */
@Service
public class PlatformUsageService {

    private final BrandService brandService;
    private final TenantService tenantService;
    private final IdentityProvisioningApi identityProvisioningApi;
    private final HealthEndpoint healthEndpoint;

    public PlatformUsageService(
            BrandService brandService,
            TenantService tenantService,
            IdentityProvisioningApi identityProvisioningApi,
            HealthEndpoint healthEndpoint) {
        this.brandService = brandService;
        this.tenantService = tenantService;
        this.identityProvisioningApi = identityProvisioningApi;
        this.healthEndpoint = healthEndpoint;
    }

    public record TenantUsage(UUID tenantId, String tenantName, String status, long activeUserCount) {}

    public record BrandUsage(UUID brandId, String brandName, long tenantCount, long activeTenantCount, long activeUserCount) {}

    public record PlatformUsage(List<BrandUsage> byBrand, long totalTenants, long totalActiveUsers, String systemHealth) {}

    @Transactional(readOnly = true)
    public PlatformUsage summarize() {
        List<Brand> brands = brandService.listAll();
        List<Tenant> tenants = tenantService.listAll();
        Map<UUID, List<Tenant>> byBrand = tenants.stream().collect(Collectors.groupingBy(Tenant::getBrandId));

        List<BrandUsage> brandUsages = brands.stream()
                .map(brand -> {
                    List<Tenant> brandTenants = byBrand.getOrDefault(brand.getId(), List.of());
                    long activeTenants = brandTenants.stream().filter(Tenant::isActive).count();
                    long activeUsers = brandTenants.stream().mapToLong(this::activeUserCount).sum();
                    return new BrandUsage(brand.getId(), brand.getName(), brandTenants.size(), activeTenants, activeUsers);
                })
                .toList();

        long totalTenants = tenants.size();
        long totalActiveUsers = brandUsages.stream().mapToLong(BrandUsage::activeUserCount).sum();
        return new PlatformUsage(brandUsages, totalTenants, totalActiveUsers, healthEndpoint.health().getStatus().getCode());
    }

    @Transactional(readOnly = true)
    public TenantUsage tenantUsage(UUID tenantId) {
        Tenant tenant = tenantService.get(tenantId);
        return new TenantUsage(tenantId, tenant.getName(), tenant.getStatus().name(), activeUserCount(tenant));
    }

    private long activeUserCount(Tenant tenant) {
        try (var scope = TenantContextScope.enter(tenant.getId())) {
            return identityProvisioningApi.countActiveUsers();
        }
    }
}
