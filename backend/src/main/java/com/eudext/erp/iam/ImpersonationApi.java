package com.eudext.erp.iam;

import java.time.Duration;
import java.util.UUID;

/** ADM-7: issues the time-boxed, scoped token a platform/brand admin uses to act as a tenant-admin. */
public interface ImpersonationApi {

    String issueImpersonationToken(UUID actorUserId, UUID targetUserId, UUID targetTenantId, Duration ttl);
}
