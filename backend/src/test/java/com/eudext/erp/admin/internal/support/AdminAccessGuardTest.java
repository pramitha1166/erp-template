package com.eudext.erp.admin.internal.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.eudext.erp.admin.PlatformIdentifiers;
import com.eudext.erp.iam.PermissionApi;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** ADM-1 / ADM-5: platform-vs-brand permission scoping — the design note both are built around. */
@ExtendWith(MockitoExtension.class)
class AdminAccessGuardTest {

    @Mock
    private PermissionApi permissionApi;

    private AdminAccessGuard guard;
    private final UUID userId = UUID.randomUUID();
    private final UUID brandId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        guard = new AdminAccessGuard(permissionApi);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requirePlatformAdminSucceedsWhenPermissionHeld() {
        when(permissionApi.hasPermission(userId, PlatformIdentifiers.PLATFORM_COMPANY_ID, AdminPermissions.PLATFORM_MANAGE))
                .thenReturn(true);

        assertThat(guard.requirePlatformAdmin(AdminPermissions.PLATFORM_MANAGE)).isEqualTo(userId);
    }

    @Test
    void requirePlatformAdminRejectsWhenPermissionMissing() {
        when(permissionApi.hasPermission(userId, PlatformIdentifiers.PLATFORM_COMPANY_ID, AdminPermissions.PLATFORM_MANAGE))
                .thenReturn(false);

        assertThatThrownBy(() -> guard.requirePlatformAdmin(AdminPermissions.PLATFORM_MANAGE))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireBrandAccessSucceedsForABrandsOwnAdmin() {
        when(permissionApi.hasPermission(userId, PlatformIdentifiers.PLATFORM_COMPANY_ID, AdminPermissions.PLATFORM_MANAGE))
                .thenReturn(false);
        when(permissionApi.hasPermission(userId, brandId, AdminPermissions.TENANT_MANAGE)).thenReturn(true);

        assertThat(guard.requireBrandAccess(brandId, AdminPermissions.TENANT_MANAGE)).isEqualTo(userId);
    }

    @Test
    void requireBrandAccessRejectsAnUnrelatedBrandsAdmin() {
        when(permissionApi.hasPermission(userId, PlatformIdentifiers.PLATFORM_COMPANY_ID, AdminPermissions.PLATFORM_MANAGE))
                .thenReturn(false);
        when(permissionApi.hasPermission(userId, brandId, AdminPermissions.TENANT_MANAGE)).thenReturn(false);

        assertThatThrownBy(() -> guard.requireBrandAccess(brandId, AdminPermissions.TENANT_MANAGE))
                .isInstanceOf(AccessDeniedException.class);
    }

    /** ADM-1's design note: a platform admin is a superset of any one brand's admin, so brand checks succeed without touching that brand's own permission. */
    @Test
    void aPlatformAdminAlwaysPassesBrandScopedChecks() {
        when(permissionApi.hasPermission(userId, PlatformIdentifiers.PLATFORM_COMPANY_ID, AdminPermissions.PLATFORM_MANAGE))
                .thenReturn(true);

        assertThat(guard.requireBrandAccess(brandId, AdminPermissions.TENANT_MANAGE)).isEqualTo(userId);
    }
}
