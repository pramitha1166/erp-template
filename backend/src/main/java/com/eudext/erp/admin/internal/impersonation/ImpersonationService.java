package com.eudext.erp.admin.internal.impersonation;

import com.eudext.erp.admin.AdminAuditEvents;
import com.eudext.erp.admin.internal.tenant.Tenant;
import com.eudext.erp.admin.internal.tenant.TenantService;
import com.eudext.erp.config.tenancy.TenantContextScope;
import com.eudext.erp.iam.IdentityProvisioningApi;
import com.eudext.erp.iam.ImpersonationApi;
import com.eudext.erp.notification.NotificationApi;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADM-7: starts/ends a time-boxed, scoped impersonation session. Every
 * session is written to {@code impersonation_sessions} and mirrored to the
 * audit trail via {@link AdminAuditEvents.ImpersonationStarted}/{@code
 * ImpersonationEnded}; per-request audit tagging for whatever the admin
 * *does* while impersonating is handled separately by {@code
 * ImpersonationContext} (see its javadoc) — this class only owns the
 * session lifecycle and token issuance.
 */
@Service
public class ImpersonationService {

    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final ImpersonationSessionRepository repository;
    private final TenantService tenantService;
    private final ImpersonationApi impersonationApi;
    private final IdentityProvisioningApi identityProvisioningApi;
    private final NotificationApi notificationApi;
    private final ApplicationEventPublisher events;

    public ImpersonationService(
            ImpersonationSessionRepository repository,
            TenantService tenantService,
            ImpersonationApi impersonationApi,
            IdentityProvisioningApi identityProvisioningApi,
            NotificationApi notificationApi,
            ApplicationEventPublisher events) {
        this.repository = repository;
        this.tenantService = tenantService;
        this.impersonationApi = impersonationApi;
        this.identityProvisioningApi = identityProvisioningApi;
        this.notificationApi = notificationApi;
        this.events = events;
    }

    public record StartedSession(UUID sessionId, String token, Instant expiresAt) {}

    @Transactional
    public StartedSession start(UUID tenantId, UUID actorUserId, String reason) {
        Tenant tenant = tenantService.get(tenantId);
        if (!tenant.isActive()) {
            throw new IllegalStateException("Cannot impersonate into a suspended tenant");
        }
        UUID targetUserId = tenant.getPrimaryAdminUserId();
        if (targetUserId == null) {
            throw new IllegalStateException("Tenant has no primary admin user to impersonate");
        }

        Instant expiresAt = Instant.now().plus(SESSION_TTL);
        String token = impersonationApi.issueImpersonationToken(actorUserId, targetUserId, tenantId, SESSION_TTL);

        UUID sessionId;
        String targetEmail;
        try (var scope = TenantContextScope.enter(tenantId)) {
            ImpersonationSession session =
                    repository.save(ImpersonationSession.start(tenantId, actorUserId, targetUserId, reason, expiresAt));
            sessionId = session.getId();
            targetEmail = identityProvisioningApi.emailOf(targetUserId);
        }

        events.publishEvent(
                new AdminAuditEvents.ImpersonationStarted(sessionId, tenantId, actorUserId, targetUserId, reason, expiresAt, Instant.now()));
        notificationApi.send(
                tenantId,
                targetEmail,
                "IMPERSONATION_STARTED",
                Map.of("reason", reason, "expiresAt", expiresAt.toString()));

        return new StartedSession(sessionId, token, expiresAt);
    }

    @Transactional
    public void end(UUID tenantId, UUID sessionId) {
        try (var scope = TenantContextScope.enter(tenantId)) {
            ImpersonationSession session =
                    repository.findById(sessionId).orElseThrow(() -> new NoSuchElementException("No such session"));
            if (session.getStatus() != ImpersonationSessionStatus.ACTIVE) {
                return;
            }
            session.end();
            repository.save(session);
            events.publishEvent(new AdminAuditEvents.ImpersonationEnded(
                    sessionId, tenantId, session.getActorUserId(), session.getTargetUserId(), Instant.now()));
        }
    }
}
