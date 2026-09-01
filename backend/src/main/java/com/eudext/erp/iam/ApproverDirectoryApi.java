package com.eudext.erp.iam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * WF-3: the lookups Epic 0.4's approver-resolution engine needs to turn a
 * configured approval step (by role, or by reporting hierarchy) into a
 * concrete set of user ids, without workflow reaching into IAM's internal
 * RBAC/user tables directly (ARCH-1). Named-user resolution needs no IAM
 * lookup at all — the configured user id is already the answer — so it has
 * no method here.
 */
public interface ApproverDirectoryApi {

    /** IAM-4: every user holding {@code roleId} in {@code companyId}, via their {@code UserCompanyRole} grants. */
    List<UUID> usersWithRole(UUID companyId, UUID roleId);

    /** WF-3: the direct manager of {@code userId}, if one is set. */
    Optional<UUID> managerOf(UUID userId);
}
