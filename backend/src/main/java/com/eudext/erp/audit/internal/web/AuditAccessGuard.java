package com.eudext.erp.audit.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.iam.PermissionApi;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Shared read-permission gate for every audit endpoint (history, search, archive status) — all admin-only, same permission code. */
@Component
class AuditAccessGuard {

    private static final String PERMISSION_READ_AUDIT_LOG = "audit:entry:read";

    private final PermissionApi permissionApi;

    AuditAccessGuard(PermissionApi permissionApi) {
        this.permissionApi = permissionApi;
    }

    void requirePermission(UUID companyId) {
        if (!permissionApi.hasPermission(currentUserId(), companyId, PERMISSION_READ_AUDIT_LOG)) {
            throw new AccessDeniedException("Missing permission: " + PERMISSION_READ_AUDIT_LOG);
        }
    }

    UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        return UUID.fromString(authentication.getName());
    }

    UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
