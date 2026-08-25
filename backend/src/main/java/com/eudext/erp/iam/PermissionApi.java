package com.eudext.erp.iam;

import java.util.UUID;

/**
 * IAM-3 / IAM-4: public entry point other modules use to check whether a
 * user holds a permission in a given company, without reaching into IAM's
 * internal RBAC tables directly (ARCH-1). No module calls this yet — real
 * business entities land starting Phase 1 — but the hook point is
 * established now so those modules don't have to touch IAM internals later.
 */
public interface PermissionApi {

    boolean hasPermission(UUID userId, UUID companyId, String permissionCode);
}
