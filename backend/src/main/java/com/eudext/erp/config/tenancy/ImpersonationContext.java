package com.eudext.erp.config.tenancy;

import java.util.Optional;
import java.util.UUID;

/**
 * ADM-7: holds the real admin user id driving the current request when
 * that request is running under an impersonation token, so audit entries
 * written during the session can be tagged as impersonated (AUD-1/AUD-2)
 * without changing who {@code SecurityContextHolder}'s authentication
 * reports as acting (that stays the impersonated tenant-admin, so ordinary
 * permission checks behave exactly as if that user were logged in
 * themselves). Set by the IAM JWT filter when it parses an impersonation
 * token, read by the audit module's generic mutation interceptor, cleared
 * in a {@code finally} — same lifecycle as {@link TenantContext}.
 */
public final class ImpersonationContext {

    private static final ThreadLocal<UUID> ACTOR = new ThreadLocal<>();

    private ImpersonationContext() {}

    public static void set(UUID actorUserId) {
        if (actorUserId == null) {
            ACTOR.remove();
        } else {
            ACTOR.set(actorUserId);
        }
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(ACTOR.get());
    }

    public static void clear() {
        ACTOR.remove();
    }
}
