package com.eudext.erp.iam;

import java.util.UUID;

/**
 * IAM-5: public entry point other modules use to ask what access a user's
 * effective roles grant on a single field of an entity (e.g. whether
 * {@code payroll:employee.salary} is readable/writable), without reaching
 * into IAM's internal tables directly (ARCH-1).
 */
public interface FieldPermissionApi {

    /**
     * Resolves the effective access for {@code fieldName} on {@code
     * entityCode} (the {@code module:entity} half of a permission triple)
     * across every role the user holds in {@code companyId}. Falls back to
     * {@link FieldAccess#WRITE} when no role has an explicit restriction on
     * the field — see the V8 migration comment for why absence means
     * unrestricted, not denied.
     */
    FieldAccess resolveAccess(UUID userId, UUID companyId, String entityCode, String fieldName);
}
