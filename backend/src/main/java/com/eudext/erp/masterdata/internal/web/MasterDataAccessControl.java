package com.eudext.erp.masterdata.internal.web;

import com.eudext.erp.iam.AuthenticationFailedException;
import com.eudext.erp.iam.PermissionApi;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * The permission-check enforcement point for master data's own REST layer — same pattern as {@code
 * com.eudext.erp.numbering.internal.web.NumberingAccessControl}: IAM's own access-control machinery is
 * module-internal (ARCH-1), so business modules read the JWT subject off {@code SecurityContextHolder} and check it
 * against {@link PermissionApi} directly.
 */
@Component
public class MasterDataAccessControl {

    private final PermissionApi permissionApi;

    public MasterDataAccessControl(PermissionApi permissionApi) {
        this.permissionApi = permissionApi;
    }

    public UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AuthenticationFailedException("Not authenticated");
        }
        return UUID.fromString(authentication.getName());
    }

    /** Returns the current user id if they hold {@code permissionCode} in {@code companyId}, otherwise throws 403. */
    public UUID requirePermission(UUID companyId, String permissionCode) {
        UUID userId = currentUserId();
        if (!permissionApi.hasPermission(userId, companyId, permissionCode)) {
            throw new AccessDeniedException("Missing permission: " + permissionCode);
        }
        return userId;
    }
}
