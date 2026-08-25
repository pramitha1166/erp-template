package com.eudext.erp.admin.internal.support;

import com.eudext.erp.admin.PlatformIdentifiers;
import com.eudext.erp.iam.PermissionApi;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * ADM-1 / ADM-5: the {@code admin} module's own enforcement point,
 * mirroring {@code iam.internal.auth.AccessControlService} (which is IAM-
 * internal and unreachable across the module boundary — see its javadoc).
 * Reads {@code SecurityContextHolder} directly rather than depending on
 * IAM's {@code CurrentUserResolver} for the same reason: that class is
 * IAM-internal, and this is a three-line read of a plain Spring Security
 * API, not an IAM concern.
 */
@Component
public class AdminAccessGuard {

    private final PermissionApi permissionApi;

    public AdminAccessGuard(PermissionApi permissionApi) {
        this.permissionApi = permissionApi;
    }

    public UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        return UUID.fromString(authentication.getName());
    }

    /** ADM-1: platform-admin-only operations (Brand CRUD, platform defaults, cross-brand reads). */
    public UUID requirePlatformAdmin(String permissionCode) {
        UUID userId = currentUserId();
        if (!permissionApi.hasPermission(userId, PlatformIdentifiers.PLATFORM_COMPANY_ID, permissionCode)) {
            throw new AccessDeniedException("Missing platform permission: " + permissionCode);
        }
        return userId;
    }

    /**
     * ADM-5: brand-scoped operations. A platform admin (holding {@link
     * AdminPermissions#PLATFORM_MANAGE}) can always act here too — a
     * platform operator is a superset of any one brand's admin, never
     * the reverse.
     */
    public UUID requireBrandAccess(UUID brandId, String permissionCode) {
        UUID userId = currentUserId();
        boolean isPlatformAdmin =
                permissionApi.hasPermission(userId, PlatformIdentifiers.PLATFORM_COMPANY_ID, AdminPermissions.PLATFORM_MANAGE);
        if (isPlatformAdmin) {
            return userId;
        }
        if (!permissionApi.hasPermission(userId, brandId, permissionCode)) {
            throw new AccessDeniedException("Missing brand permission: " + permissionCode);
        }
        return userId;
    }
}
