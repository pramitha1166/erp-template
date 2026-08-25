package com.eudext.erp.iam.internal.auth;

import com.eudext.erp.iam.PermissionApi;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * IAM-3 / IAM-4: the enforcement point IAM's own REST layer uses to gate
 * role/permission management by the caller's own permissions, in whatever
 * company the operation is scoped to. Business modules from Phase 1
 * onward will do the same against {@link PermissionApi} directly rather
 * than through this class, which is IAM-internal.
 */
@Component
public class AccessControlService {

    private final PermissionApi permissionApi;
    private final CurrentUserResolver currentUserResolver;

    public AccessControlService(PermissionApi permissionApi, CurrentUserResolver currentUserResolver) {
        this.permissionApi = permissionApi;
        this.currentUserResolver = currentUserResolver;
    }

    /** Returns the current user id if they hold {@code permissionCode} in {@code companyId}, otherwise throws 403. */
    public UUID requirePermission(UUID companyId, String permissionCode) {
        UUID userId = currentUserResolver.currentUserId();
        if (!permissionApi.hasPermission(userId, companyId, permissionCode)) {
            throw new AccessDeniedException("Missing permission: " + permissionCode);
        }
        return userId;
    }
}
