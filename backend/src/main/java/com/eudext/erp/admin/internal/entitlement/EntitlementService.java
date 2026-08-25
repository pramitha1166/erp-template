package com.eudext.erp.admin.internal.entitlement;

import com.eudext.erp.config.tenancy.TenantContextScope;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADM-1 / ADM-5 / BRD-12: resolves the three-tier entitlement chain
 * (platform default → brand override → tenant override) and enforces that
 * a tenant can never be granted more than its brand itself has (ADM-5).
 */
@Service
public class EntitlementService {

    private final PlatformEntitlementDefaultRepository platformDefaults;
    private final BrandEntitlementRepository brandEntitlements;
    private final TenantEntitlementRepository tenantEntitlements;

    public EntitlementService(
            PlatformEntitlementDefaultRepository platformDefaults,
            BrandEntitlementRepository brandEntitlements,
            TenantEntitlementRepository tenantEntitlements) {
        this.platformDefaults = platformDefaults;
        this.brandEntitlements = brandEntitlements;
        this.tenantEntitlements = tenantEntitlements;
    }

    @Transactional
    public void setPlatformDefault(String featureCode, boolean enabled) {
        PlatformEntitlementDefault entry = platformDefaults
                .findByFeatureCode(featureCode)
                .orElseGet(() -> PlatformEntitlementDefault.of(featureCode, enabled));
        entry.setEnabled(enabled);
        platformDefaults.save(entry);
    }

    @Transactional(readOnly = true)
    public List<PlatformEntitlementDefault> listPlatformDefaults() {
        return platformDefaults.findAllByOrderByFeatureCode();
    }

    @Transactional
    public void setBrandEntitlement(UUID brandId, String featureCode, boolean enabled) {
        BrandEntitlement entry = brandEntitlements
                .findByBrandIdAndFeatureCode(brandId, featureCode)
                .orElseGet(() -> BrandEntitlement.of(brandId, featureCode, enabled));
        entry.setEnabled(enabled);
        brandEntitlements.save(entry);
    }

    @Transactional(readOnly = true)
    public boolean resolveBrandEntitlement(UUID brandId, String featureCode) {
        return brandEntitlements
                .findByBrandIdAndFeatureCode(brandId, featureCode)
                .map(BrandEntitlement::isEnabled)
                .orElseGet(() -> platformDefaults.findByFeatureCode(featureCode).map(PlatformEntitlementDefault::isEnabled).orElse(false));
    }

    /**
     * @throws EntitlementBoundExceededException if {@code enabled} is true but the brand itself doesn't have this feature (ADM-5)
     */
    @Transactional
    public void setTenantEntitlement(UUID brandId, UUID tenantId, String featureCode, boolean enabled) {
        if (enabled && !resolveBrandEntitlement(brandId, featureCode)) {
            throw new EntitlementBoundExceededException(featureCode);
        }
        try (var scope = TenantContextScope.enter(tenantId)) {
            TenantEntitlement entry = tenantEntitlements
                    .findByTenantIdAndFeatureCode(tenantId, featureCode)
                    .orElseGet(() -> TenantEntitlement.of(tenantId, featureCode, enabled));
            entry.setEnabled(enabled);
            tenantEntitlements.save(entry);
        }
    }

    @Transactional(readOnly = true)
    public boolean resolveTenantEntitlement(UUID brandId, UUID tenantId, String featureCode) {
        try (var scope = TenantContextScope.enter(tenantId)) {
            return tenantEntitlements
                    .findByTenantIdAndFeatureCode(tenantId, featureCode)
                    .map(TenantEntitlement::isEnabled)
                    .orElseGet(() -> resolveBrandEntitlement(brandId, featureCode));
        }
    }

    @Transactional(readOnly = true)
    public List<TenantEntitlement> listTenantEntitlements(UUID tenantId) {
        try (var scope = TenantContextScope.enter(tenantId)) {
            return tenantEntitlements.findByTenantId(tenantId);
        }
    }
}
