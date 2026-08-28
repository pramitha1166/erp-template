package com.eudext.erp.admin.internal.invite;

import com.eudext.erp.config.tenancy.TenantContextScope;
import com.eudext.erp.iam.IdentityProvisioningApi;
import com.eudext.erp.notification.NotificationApi;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ADM-5 / BRD-14: brand-admin invite flow for additional/replacement tenant-admin users. */
@Service
public class TenantAdminInviteService {

    private static final Duration INVITE_TTL = Duration.ofDays(7);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TenantAdminInviteRepository repository;
    private final IdentityProvisioningApi identityProvisioningApi;
    private final NotificationApi notificationApi;

    public TenantAdminInviteService(
            TenantAdminInviteRepository repository, IdentityProvisioningApi identityProvisioningApi, NotificationApi notificationApi) {
        this.repository = repository;
        this.identityProvisioningApi = identityProvisioningApi;
        this.notificationApi = notificationApi;
    }

    @Transactional
    public void invite(UUID tenantId, String email, String invitedBy) {
        try (var scope = TenantContextScope.enter(tenantId)) {
            String rawToken = generateToken();
            repository.save(TenantAdminInvite.create(tenantId, email, hash(rawToken), invitedBy, Instant.now().plus(INVITE_TTL)));
            notificationApi.send(tenantId, email, "TENANT_ADMIN_INVITE", Map.of("inviteToken", rawToken));
        }
    }

    @Transactional(readOnly = true)
    public List<TenantAdminInvite> list(UUID tenantId) {
        try (var scope = TenantContextScope.enter(tenantId)) {
            return repository.findByTenantId(tenantId);
        }
    }

    /**
     * Accepting an invite means resolving the tenant purely from the
     * token's hash — the caller has no session yet, so no {@code
     * TenantContext} is set until this lookup itself establishes it. The
     * token hash is looked up in the ambient tenant... except there isn't
     * one yet: {@code tenant_admin_invites} intentionally has no
     * cross-tenant unique-token lookup path other than iterating known
     * tenants, mirroring the same "caller supplies the scope, RLS still
     * protects it" precedent as login (V4 migration). The web layer is
     * expected to route acceptance by a tenant-scoped link (tenantId in
     * the URL), so this takes it explicitly rather than searching blindly.
     */
    @Transactional
    public UUID accept(UUID tenantId, String rawToken, String chosenPassword) {
        try (var scope = TenantContextScope.enter(tenantId)) {
            TenantAdminInvite invite = repository
                    .findByTokenHash(hash(rawToken))
                    .filter(candidate -> candidate.getTenantId().equals(tenantId))
                    .orElseThrow(() -> new NoSuchElementException("Invalid or unknown invite"));
            if (invite.getStatus() != InviteStatus.PENDING) {
                throw new IllegalStateException("Invite is no longer pending");
            }
            if (invite.isExpired()) {
                invite.expire();
                repository.save(invite);
                throw new IllegalStateException("Invite has expired");
            }

            IdentityProvisioningApi.ProvisionedUser user =
                    identityProvisioningApi.provisionTenantUser(tenantId, invite.getEmail(), chosenPassword);
            invite.accept();
            repository.save(invite);
            return user.userId();
        }
    }

    @Transactional
    public void revoke(UUID tenantId, UUID inviteId) {
        try (var scope = TenantContextScope.enter(tenantId)) {
            repository.findById(inviteId).ifPresent(invite -> {
                invite.revoke();
                repository.save(invite);
            });
        }
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
