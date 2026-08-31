package com.eudext.erp.admin.internal.tenant;

import com.eudext.erp.admin.AdminAuditEvents;
import com.eudext.erp.admin.internal.brand.Brand;
import com.eudext.erp.admin.internal.brand.BrandService;
import com.eudext.erp.admin.internal.checklist.ChecklistService;
import com.eudext.erp.admin.internal.entitlement.EntitlementService;
import com.eudext.erp.config.tenancy.TenantContextScope;
import com.eudext.erp.iam.IdentityProvisioningApi;
import com.eudext.erp.masterdata.MasterDataProvisioningApi;
import com.eudext.erp.notification.NotificationApi;
import com.eudext.erp.numbering.NumberingApi;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADM-2 / ADM-3: creates a Tenant, its first Company, initial tenant-admin
 * user, entitlement assignment, and onboarding seed data as one
 * transactional flow — the workflow Acceptance Criterion 1 ("zero to first
 * invoice in under 4 hours") depends on. Every write here after the {@code
 * Tenant} row itself is created runs inside a single {@link
 * TenantContextScope} for the new tenant, since the calling platform/brand
 * admin's own ambient tenant context is the sentinel platform tenant, not
 * this one (see {@code PlatformIdentifiers}).
 */
@Service
public class TenantOnboardingService {

    private static final List<String> TENANT_ADMIN_PERMISSIONS =
            List.of("iam:user:create", "iam:role:create", "iam:role:manage", "iam:user-role:assign");

    private final TenantRepository tenantRepository;
    private final BrandService brandService;
    private final EntitlementService entitlementService;
    private final ChecklistService checklistService;
    private final MasterDataProvisioningApi masterDataApi;
    private final NumberingApi numberingApi;
    private final IdentityProvisioningApi identityProvisioningApi;
    private final NotificationApi notificationApi;
    private final ApplicationEventPublisher events;

    public TenantOnboardingService(
            TenantRepository tenantRepository,
            BrandService brandService,
            EntitlementService entitlementService,
            ChecklistService checklistService,
            MasterDataProvisioningApi masterDataApi,
            NumberingApi numberingApi,
            IdentityProvisioningApi identityProvisioningApi,
            NotificationApi notificationApi,
            ApplicationEventPublisher events) {
        this.tenantRepository = tenantRepository;
        this.brandService = brandService;
        this.entitlementService = entitlementService;
        this.checklistService = checklistService;
        this.masterDataApi = masterDataApi;
        this.numberingApi = numberingApi;
        this.identityProvisioningApi = identityProvisioningApi;
        this.notificationApi = notificationApi;
        this.events = events;
    }

    public record Request(
            String tenantName,
            MasterDataProvisioningApi.NewCompany company,
            String adminEmail,
            Set<String> initialEntitlementFeatureCodes) {}

    @Transactional
    public Tenant onboard(UUID brandId, Request request, String actor) {
        Brand brand = brandService.get(brandId);
        if (!brand.isActive()) {
            throw new IllegalStateException("Cannot onboard a tenant under a suspended brand");
        }
        if (identityProvisioningApi.emailInUse(request.adminEmail())) {
            throw new IllegalArgumentException("Email is already in use: " + request.adminEmail());
        }

        Tenant tenant = tenantRepository.save(Tenant.create(brandId, request.tenantName()));
        UUID tenantId = tenant.getId();

        try (var scope = TenantContextScope.enter(tenantId)) {
            for (String featureCode : request.initialEntitlementFeatureCodes()) {
                entitlementService.setTenantEntitlement(brandId, tenantId, featureCode, true);
            }
            boolean lkEntitled = request.initialEntitlementFeatureCodes().contains("MOD-LK");

            MasterDataProvisioningApi.CompanyView company = masterDataApi.createCompany(tenantId, request.company());
            masterDataApi.seedDefaultChartOfAccounts(tenantId, company.id(), lkEntitled);
            LocalDate fiscalYearStart = defaultFiscalYearStart(request.company().fiscalYearStartMonth());
            masterDataApi.seedDefaultFiscalYear(tenantId, company.id(), fiscalYearStart);
            numberingApi.seedDefaultSeries(tenantId, company.id());

            IdentityProvisioningApi.ProvisionedUser admin = identityProvisioningApi.provisionTenantUser(tenantId, request.adminEmail());
            UUID roleId = identityProvisioningApi.createRole(tenantId, "Tenant Administrator", "Full access, created at onboarding");
            TENANT_ADMIN_PERMISSIONS.forEach(code -> identityProvisioningApi.grantPermission(tenantId, roleId, code));
            identityProvisioningApi.assignRole(tenantId, admin.userId(), company.id(), roleId, actor);

            tenant.assignPrimaryCompany(company.id());
            tenant.assignPrimaryAdmin(admin.userId());
            tenantRepository.save(tenant);

            checklistService.seedDefaults(tenantId);

            notificationApi.send(
                    tenantId,
                    admin.email(),
                    "TENANT_WELCOME",
                    Map.of(
                            "tenantName", request.tenantName(),
                            "companyName", company.legalName(),
                            "temporaryPassword", admin.temporaryPassword()));
        }

        events.publishEvent(new AdminAuditEvents.TenantOnboarded(tenantId, brandId, tenant.getPrimaryCompanyId(), actor, Instant.now()));
        return tenant;
    }

    private static LocalDate defaultFiscalYearStart(int fiscalYearStartMonth) {
        LocalDate today = LocalDate.now();
        LocalDate start = LocalDate.of(today.getYear(), fiscalYearStartMonth, 1);
        return start.isAfter(today) ? start.minusYears(1) : start;
    }
}
