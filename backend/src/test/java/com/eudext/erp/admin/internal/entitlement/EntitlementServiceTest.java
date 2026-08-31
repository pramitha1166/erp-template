package com.eudext.erp.admin.internal.entitlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.eudext.erp.config.tenancy.TenantContext;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ADM-1 / ADM-5 / BRD-12: the three-tier entitlement resolution and the "never beyond the brand's own bound" rule. */
@ExtendWith(MockitoExtension.class)
class EntitlementServiceTest {

    @Mock
    private PlatformEntitlementDefaultRepository platformDefaults;

    @Mock
    private BrandEntitlementRepository brandEntitlements;

    @Mock
    private TenantEntitlementRepository tenantEntitlements;

    private EntitlementService service;
    private final UUID brandId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new EntitlementService(platformDefaults, brandEntitlements, tenantEntitlements);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void brandEntitlementFallsBackToPlatformDefaultWhenNoOverrideExists() {
        when(brandEntitlements.findByBrandIdAndFeatureCode(brandId, "MOD-LK")).thenReturn(Optional.empty());
        when(platformDefaults.findByFeatureCode("MOD-LK")).thenReturn(Optional.of(PlatformEntitlementDefault.of("MOD-LK", true)));

        assertThat(service.resolveBrandEntitlement(brandId, "MOD-LK")).isTrue();
    }

    @Test
    void brandEntitlementOverridesThePlatformDefault() {
        when(brandEntitlements.findByBrandIdAndFeatureCode(brandId, "MOD-LK"))
                .thenReturn(Optional.of(BrandEntitlement.of(brandId, "MOD-LK", false)));

        assertThat(service.resolveBrandEntitlement(brandId, "MOD-LK")).isFalse();
    }

    @Test
    void noPlatformDefaultResolvesToDisabled() {
        when(brandEntitlements.findByBrandIdAndFeatureCode(brandId, "PAYROLL")).thenReturn(Optional.empty());
        when(platformDefaults.findByFeatureCode("PAYROLL")).thenReturn(Optional.empty());

        assertThat(service.resolveBrandEntitlement(brandId, "PAYROLL")).isFalse();
    }

    @Test
    void settingATenantEntitlementBeyondTheBrandsOwnBoundIsRejected() {
        when(brandEntitlements.findByBrandIdAndFeatureCode(brandId, "PAYROLL")).thenReturn(Optional.empty());
        when(platformDefaults.findByFeatureCode("PAYROLL")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setTenantEntitlement(brandId, tenantId, "PAYROLL", true))
                .isInstanceOf(EntitlementBoundExceededException.class);
    }

    @Test
    void settingATenantEntitlementWithinTheBrandsBoundSucceeds() {
        when(brandEntitlements.findByBrandIdAndFeatureCode(brandId, "MOD-LK"))
                .thenReturn(Optional.of(BrandEntitlement.of(brandId, "MOD-LK", true)));
        when(tenantEntitlements.findByTenantIdAndFeatureCode(tenantId, "MOD-LK")).thenReturn(Optional.empty());

        service.setTenantEntitlement(brandId, tenantId, "MOD-LK", true);
    }

    @Test
    void disablingATenantEntitlementNeverNeedsTheBoundCheck() {
        when(tenantEntitlements.findByTenantIdAndFeatureCode(tenantId, "MOD-LK")).thenReturn(Optional.empty());

        service.setTenantEntitlement(brandId, tenantId, "MOD-LK", false);
    }
}
