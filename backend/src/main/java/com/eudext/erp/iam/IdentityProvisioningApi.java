package com.eudext.erp.iam;

import java.util.UUID;

/**
 * ADM-2 / ADM-5 / ADM-6 / ADM-9: the privileged provisioning surface Epic
 * 0.11's tenant onboarding and admin consoles use to create a tenant's
 * first user and role, invite additional users, and read/toggle account
 * state — all things a brand-new tenant has no user yet able to do through
 * the ordinary {@code iam:user:create}-gated {@code /iam/users} endpoint
 * (see that controller's javadoc). Kept separate from {@link PermissionApi}
 * because it mutates identity state rather than just checking it, and
 * callers here are trusted admin-console flows, not arbitrary business
 * modules.
 */
public interface IdentityProvisioningApi {

    /**
     * Creates a user in {@code tenantId} with a freshly generated temporary
     * password, returned once so the caller can deliver it (e.g. via
     * {@code NotificationApi}) — it is never logged or persisted in the
     * clear beyond this call. IAM-9: the account is marked
     * must-change-password, so the next successful login reports {@code
     * passwordChangeRequired = true} regardless of the tenant's expiry
     * policy.
     */
    ProvisionedUser provisionTenantUser(UUID tenantId, String email);

    /** ADM-5: for invite acceptance, where the invitee (not the system) chooses their own password. */
    ProvisionedUser provisionTenantUser(UUID tenantId, String email, String chosenPassword);

    boolean emailInUse(String email);

    String emailOf(UUID userId);

    UUID createRole(UUID tenantId, String name, String description);

    void grantPermission(UUID tenantId, UUID roleId, String permissionCode);

    void assignRole(UUID tenantId, UUID userId, UUID companyId, UUID roleId, String assignedBy);

    void setUserActive(UUID tenantId, UUID userId, boolean active);

    /** WF-3: sets (or clears, with a {@code null} managerId) the direct manager used for reporting-hierarchy approver resolution. */
    void setManager(UUID tenantId, UUID userId, UUID managerId);

    /** Active-user count for whichever tenant is ambient in {@code TenantContext} when called. */
    long countActiveUsers();

    /** True if any user holds {@code permissionCode} scoped to {@code companyId} — used to gate one-time bootstrap flows. */
    boolean anyUserHoldsPermission(UUID companyId, String permissionCode);

    record ProvisionedUser(UUID userId, String email, String temporaryPassword) {}
}
