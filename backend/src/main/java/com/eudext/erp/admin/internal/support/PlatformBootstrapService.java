package com.eudext.erp.admin.internal.support;

import com.eudext.erp.admin.PlatformIdentifiers;
import com.eudext.erp.config.tenancy.TenantContextScope;
import com.eudext.erp.iam.IdentityProvisioningApi;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADM-1: the classic "first run creates the admin" bootstrap — there is no
 * seeded platform-admin credential (never hardcode one, BRD-2/NFR-S* spirit
 * applies just as much to ops secrets as to branding), so this creates
 * exactly one platform admin, and only while none exists yet. Once any
 * user holds {@link AdminPermissions#PLATFORM_MANAGE}, every subsequent
 * call is rejected — further platform admins are created the ordinary way,
 * by an existing platform admin inviting one (not modeled yet; out of this
 * epic's minimum scope).
 */
@Service
public class PlatformBootstrapService {

    private final IdentityProvisioningApi identityProvisioningApi;

    public PlatformBootstrapService(IdentityProvisioningApi identityProvisioningApi) {
        this.identityProvisioningApi = identityProvisioningApi;
    }

    @Transactional
    public IdentityProvisioningApi.ProvisionedUser bootstrapFirstPlatformAdmin(String email) {
        try (var scope = TenantContextScope.enter(PlatformIdentifiers.PLATFORM_TENANT_ID)) {
            if (identityProvisioningApi.anyUserHoldsPermission(PlatformIdentifiers.PLATFORM_COMPANY_ID, AdminPermissions.PLATFORM_MANAGE)) {
                throw new IllegalStateException("A platform admin already exists");
            }
            IdentityProvisioningApi.ProvisionedUser user =
                    identityProvisioningApi.provisionTenantUser(PlatformIdentifiers.PLATFORM_TENANT_ID, email);
            UUID roleId = identityProvisioningApi.createRole(
                    PlatformIdentifiers.PLATFORM_TENANT_ID, "Platform Administrator", "Bootstrap platform admin role");
            identityProvisioningApi.grantPermission(PlatformIdentifiers.PLATFORM_TENANT_ID, roleId, AdminPermissions.PLATFORM_MANAGE);
            identityProvisioningApi.assignRole(
                    PlatformIdentifiers.PLATFORM_TENANT_ID,
                    user.userId(),
                    PlatformIdentifiers.PLATFORM_COMPANY_ID,
                    roleId,
                    "system:bootstrap");
            return user;
        }
    }
}
