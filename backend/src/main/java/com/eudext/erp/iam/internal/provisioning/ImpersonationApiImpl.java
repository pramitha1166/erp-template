package com.eudext.erp.iam.internal.provisioning;

import com.eudext.erp.iam.ImpersonationApi;
import com.eudext.erp.iam.internal.auth.JwtService;
import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class ImpersonationApiImpl implements ImpersonationApi {

    private final JwtService jwtService;

    ImpersonationApiImpl(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public String issueImpersonationToken(UUID actorUserId, UUID targetUserId, UUID targetTenantId, Duration ttl) {
        return jwtService.issueImpersonationToken(actorUserId, targetUserId, targetTenantId, ttl);
    }
}
