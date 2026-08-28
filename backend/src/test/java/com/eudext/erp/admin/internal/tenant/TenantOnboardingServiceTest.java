package com.eudext.erp.admin.internal.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eudext.erp.admin.internal.brand.Brand;
import com.eudext.erp.admin.internal.brand.BrandService;
import com.eudext.erp.admin.internal.checklist.ChecklistService;
import com.eudext.erp.admin.internal.entitlement.EntitlementService;
import com.eudext.erp.iam.IdentityProvisioningApi;
import com.eudext.erp.masterdata.MasterDataProvisioningApi;
import com.eudext.erp.notification.NotificationApi;
import com.eudext.erp.numbering.NumberingApi;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * ADM-2 / ADM-3: the tenant onboarding orchestration that Acceptance
 * Criterion 1 ("zero to first invoice in under 4 hours") depends on.
 */
@ExtendWith(MockitoExtension.class)
class TenantOnboardingServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private BrandService brandService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ChecklistService checklistService;

    @Mock
    private MasterDataProvisioningApi masterDataApi;

    @Mock
    private NumberingApi numberingApi;

    @Mock
    private IdentityProvisioningApi identityProvisioningApi;

    @Mock
    private NotificationApi notificationApi;

    @Mock
    private ApplicationEventPublisher events;

    private TenantOnboardingService service;
    private final UUID brandId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID adminUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TenantOnboardingService(
                tenantRepository,
                brandService,
                entitlementService,
                checklistService,
                masterDataApi,
                numberingApi,
                identityProvisioningApi,
                notificationApi,
                events);
    }

    private TenantOnboardingService.Request happyPathRequest(Set<String> entitlements) {
        var company = new MasterDataProvisioningApi.NewCompany("Acme Lanka Pvt Ltd", "REG-1", "VAT-1", "Colombo", "LKR", 1);
        return new TenantOnboardingService.Request("Acme Lanka", company, "admin@acme.test", entitlements);
    }

    @Test
    void rejectsOnboardingUnderASuspendedBrand() {
        Brand brand = Brand.create("Acme", "Acme Pvt Ltd", "support@acme.test");
        brand.suspend("non-payment");
        when(brandService.get(brandId)).thenReturn(brand);

        assertThatThrownBy(() -> service.onboard(brandId, happyPathRequest(Set.of()), "actor"))
                .isInstanceOf(IllegalStateException.class);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void rejectsOnboardingWhenTheAdminEmailIsAlreadyInUse() {
        when(brandService.get(brandId)).thenReturn(Brand.create("Acme", "Acme Pvt Ltd", "support@acme.test"));
        when(identityProvisioningApi.emailInUse("admin@acme.test")).thenReturn(true);

        assertThatThrownBy(() -> service.onboard(brandId, happyPathRequest(Set.of()), "actor"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void happyPathCreatesCompanySeedsDataProvisionsAdminAndPublishesOnboardedEvent() {
        when(brandService.get(brandId)).thenReturn(Brand.create("Acme", "Acme Pvt Ltd", "support@acme.test"));
        when(identityProvisioningApi.emailInUse(any())).thenReturn(false);
        when(tenantRepository.save(any())).thenAnswer(inv -> withGeneratedId(inv.getArgument(0)));

        MasterDataProvisioningApi.CompanyView companyView = new MasterDataProvisioningApi.CompanyView(companyId, "Acme Lanka Pvt Ltd", "LKR", false);
        when(masterDataApi.createCompany(any(), any())).thenReturn(companyView);

        UUID roleId = UUID.randomUUID();
        when(identityProvisioningApi.provisionTenantUser(any(), eq("admin@acme.test")))
                .thenReturn(new IdentityProvisioningApi.ProvisionedUser(adminUserId, "admin@acme.test", "tempPass123!"));
        when(identityProvisioningApi.createRole(any(), eq("Tenant Administrator"), any())).thenReturn(roleId);

        Tenant tenant = service.onboard(brandId, happyPathRequest(Set.of("MOD-LK")), "actor");

        assertThat(tenant.getPrimaryCompanyId()).isEqualTo(companyId);
        assertThat(tenant.getPrimaryAdminUserId()).isEqualTo(adminUserId);

        verify(entitlementService).setTenantEntitlement(brandId, tenant.getId(), "MOD-LK", true);
        verify(masterDataApi).seedDefaultChartOfAccounts(tenant.getId(), companyId, true);
        verify(masterDataApi).seedDefaultFiscalYear(eq(tenant.getId()), eq(companyId), any());
        verify(numberingApi).seedDefaultSeries(tenant.getId(), companyId);
        verify(identityProvisioningApi, times(4)).grantPermission(eq(tenant.getId()), eq(roleId), any());
        verify(identityProvisioningApi).assignRole(tenant.getId(), adminUserId, companyId, roleId, "actor");
        verify(checklistService).seedDefaults(tenant.getId());
        verify(notificationApi).send(eq(tenant.getId()), eq("admin@acme.test"), eq("TENANT_WELCOME"), any());
        verify(events).publishEvent(any(Object.class));
    }

    @Test
    void skipsSriLankaChartOfAccountsWhenModLkIsNotAmongTheInitialEntitlements() {
        when(brandService.get(brandId)).thenReturn(Brand.create("Acme", "Acme Pvt Ltd", "support@acme.test"));
        when(identityProvisioningApi.emailInUse(any())).thenReturn(false);
        when(tenantRepository.save(any())).thenAnswer(inv -> withGeneratedId(inv.getArgument(0)));
        when(masterDataApi.createCompany(any(), any()))
                .thenReturn(new MasterDataProvisioningApi.CompanyView(companyId, "Acme Lanka Pvt Ltd", "LKR", false));
        when(identityProvisioningApi.provisionTenantUser(any(), any()))
                .thenReturn(new IdentityProvisioningApi.ProvisionedUser(adminUserId, "admin@acme.test", "tempPass123!"));
        when(identityProvisioningApi.createRole(any(), any(), any())).thenReturn(UUID.randomUUID());

        service.onboard(brandId, happyPathRequest(Set.of("INVENTORY")), "actor");

        verify(masterDataApi).seedDefaultChartOfAccounts(any(), eq(companyId), eq(false));
    }

    private static Tenant withGeneratedId(Tenant tenant) {
        try {
            var field = Tenant.class.getDeclaredField("id");
            field.setAccessible(true);
            if (field.get(tenant) == null) {
                field.set(tenant, UUID.randomUUID());
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return tenant;
    }
}
