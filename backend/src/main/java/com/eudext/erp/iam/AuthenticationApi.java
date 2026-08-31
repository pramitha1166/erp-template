package com.eudext.erp.iam;

import java.util.UUID;

/**
 * ADM-1 / ADM-5: the email/password + TOTP login flow, exposed so the
 * {@code admin} module's own realm-specific entry point (platform/brand
 * admin login, scoped to {@code PlatformIdentifiers.PLATFORM_TENANT_ID})
 * can reuse the exact same credential-check, lockout, MFA, and session
 * issuance logic {@code /auth/login} uses for tenant users — without
 * threading a caller-supplied {@code tenantId} through the tenant login
 * contract itself (see the design note on Epic 0.11's admin auth task) and
 * without reaching into {@code iam.internal.auth.AuthService} directly,
 * which the module boundary forbids.
 */
public interface AuthenticationApi {

    LoginOutcome login(UUID tenantId, String email, String rawPassword, String ipAddress, String userAgent);

    record LoginOutcome(
            boolean mfaRequired,
            String mfaChallengeToken,
            String accessToken,
            String refreshToken,
            boolean passwordChangeRequired) {}
}
