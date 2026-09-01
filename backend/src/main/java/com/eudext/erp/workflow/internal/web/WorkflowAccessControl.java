package com.eudext.erp.workflow.internal.web;

import com.eudext.erp.iam.AuthenticationFailedException;
import com.eudext.erp.iam.PermissionApi;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * The permission-check enforcement point for workflow's own REST layer.
 * IAM's equivalent ({@code AccessControlService}/{@code CurrentUserResolver})
 * is module-internal (ARCH-1), so — per its own javadoc, which says exactly
 * this is expected of business modules from Phase 1 onward — workflow reads
 * the JWT subject (a user id) straight off {@code SecurityContextHolder}
 * itself and checks it against {@link PermissionApi} directly.
 */
@Component
public class WorkflowAccessControl {

    private final PermissionApi permissionApi;

    public WorkflowAccessControl(PermissionApi permissionApi) {
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
