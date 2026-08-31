package com.eudext.erp.admin.internal.brand;

import com.eudext.erp.admin.AdminAuditEvents;
import com.eudext.erp.admin.PlatformIdentifiers;
import com.eudext.erp.admin.internal.support.AdminPermissions;
import com.eudext.erp.config.tenancy.TenantContextScope;
import com.eudext.erp.iam.IdentityProvisioningApi;
import com.eudext.erp.notification.NotificationApi;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ADM-1: Brand CRUD for the Platform Admin Console. */
@Service
public class BrandService {

    private final BrandRepository brandRepository;
    private final IdentityProvisioningApi identityProvisioningApi;
    private final NotificationApi notificationApi;
    private final ApplicationEventPublisher events;

    public BrandService(
            BrandRepository brandRepository,
            IdentityProvisioningApi identityProvisioningApi,
            NotificationApi notificationApi,
            ApplicationEventPublisher events) {
        this.brandRepository = brandRepository;
        this.identityProvisioningApi = identityProvisioningApi;
        this.notificationApi = notificationApi;
        this.events = events;
    }

    @Transactional
    public Brand create(String name, String legalName, String supportEmail, String createdBy) {
        Brand brand = brandRepository.save(Brand.create(name, legalName, supportEmail));
        events.publishEvent(new AdminAuditEvents.BrandCreated(brand.getId(), createdBy, Instant.now()));
        return brand;
    }

    @Transactional(readOnly = true)
    public Brand get(UUID brandId) {
        return brandRepository.findById(brandId).orElseThrow(() -> new NoSuchElementException("No such brand"));
    }

    @Transactional(readOnly = true)
    public List<Brand> listAll() {
        return brandRepository.findAll();
    }

    @Transactional
    public void suspend(UUID brandId, String reason, String actor) {
        Brand brand = get(brandId);
        if (!brand.isActive()) {
            return;
        }
        brand.suspend(reason);
        brandRepository.save(brand);
        events.publishEvent(new AdminAuditEvents.BrandSuspended(brandId, reason, actor, Instant.now()));
    }

    @Transactional
    public void reactivate(UUID brandId, String actor) {
        Brand brand = get(brandId);
        if (brand.isActive()) {
            return;
        }
        brand.reactivate();
        brandRepository.save(brand);
        events.publishEvent(new AdminAuditEvents.BrandReactivated(brandId, actor, Instant.now()));
    }

    /**
     * ADM-5 / BRD-14: provisions the first Brand Admin Console user for a
     * Brand, homed under the same platform sentinel tenant as platform
     * staff but scoped to this brand (see {@code PlatformIdentifiers}).
     */
    @Transactional
    public IdentityProvisioningApi.ProvisionedUser provisionBrandAdmin(UUID brandId, String email, String actor) {
        Brand brand = get(brandId);
        try (var scope = TenantContextScope.enter(PlatformIdentifiers.PLATFORM_TENANT_ID)) {
            IdentityProvisioningApi.ProvisionedUser user =
                    identityProvisioningApi.provisionTenantUser(PlatformIdentifiers.PLATFORM_TENANT_ID, email);
            UUID roleId = identityProvisioningApi.createRole(
                    PlatformIdentifiers.PLATFORM_TENANT_ID, "Brand Administrator", "Brand Admin Console access for " + brand.getName());
            identityProvisioningApi.grantPermission(PlatformIdentifiers.PLATFORM_TENANT_ID, roleId, AdminPermissions.BRAND_MANAGE);
            identityProvisioningApi.assignRole(PlatformIdentifiers.PLATFORM_TENANT_ID, user.userId(), brandId, roleId, actor);
            notificationApi.send(
                    null,
                    user.email(),
                    "BRAND_ADMIN_WELCOME",
                    Map.of("brandName", brand.getName(), "temporaryPassword", user.temporaryPassword()));
            return user;
        }
    }
}
