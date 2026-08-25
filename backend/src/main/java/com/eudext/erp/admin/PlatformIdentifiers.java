package com.eudext.erp.admin;

import java.util.UUID;

/**
 * ADM-1 / ADM-5: platform and brand admin staff are Eudext operators or
 * reseller-partner staff — not users of any one customer Tenant — but
 * {@code iam.User} requires a {@code tenant_id} (V4 migration: every login
 * is scoped to a tenant) and {@code PermissionApi.hasPermission} is keyed
 * on {@code (userId, companyId)} via {@code UserCompanyRole}.
 *
 * <p>Rather than adding a second identity model, admin/brand staff are
 * ordinary {@code iam.User} rows homed under this fixed sentinel tenant.
 * Within it, "company" is repurposed per role:
 *
 * <ul>
 *   <li>A platform admin holds their role scoped to {@link #PLATFORM_COMPANY_ID}.
 *   <li>A brand admin holds their role scoped to the {@code Brand}'s own id
 *       used as the "company" — which is exactly what makes {@code
 *       UserRoleAssignmentService}'s existing "different role per company"
 *       semantics (IAM-4) double as "different role per brand" with zero
 *       IAM schema changes, and lets one person administer more than one
 *       brand by holding more than one such assignment.
 * </ul>
 *
 * <p>This is an interim mechanism, same spirit as the caller-supplied
 * {@code tenantId} at login documented on {@code users} — it holds until
 * Epic 0.9 gives the platform a real staff-identity model.
 */
public final class PlatformIdentifiers {

    public static final UUID PLATFORM_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-00000000adb1");
    public static final UUID PLATFORM_COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-00000000adb2");

    private PlatformIdentifiers() {}
}
