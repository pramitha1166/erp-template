package com.eudext.erp.iam.internal.auth;

import com.eudext.erp.config.tenancy.SuspendedTenantRegistry;
import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.config.tenancy.TenantSuspendedException;
import com.eudext.erp.iam.AuthAuditEvents;
import com.eudext.erp.iam.AuthenticationFailedException;
import com.eudext.erp.iam.internal.session.SessionService;
import com.eudext.erp.iam.internal.settings.SecurityPolicy;
import com.eudext.erp.iam.internal.settings.TenantSecuritySettingsService;
import com.eudext.erp.iam.internal.totp.TotpService;
import com.eudext.erp.iam.internal.user.User;
import com.eudext.erp.iam.internal.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IAM-1 / IAM-2: the login/refresh/logout/2FA flows. Owns the one place in
 * the codebase where {@code TenantContext} is set from a client-supplied
 * value rather than an already-validated JWT — see the V4 migration
 * comment on why that's an acceptable, deliberate exception to "never
 * trust a client-supplied tenant id".
 */
@Service
public class AuthService {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final TotpService totpService;
    private final TenantSecuritySettingsService settingsService;
    private final SuspendedTenantRegistry suspendedTenantRegistry;
    private final ApplicationEventPublisher events;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SessionService sessionService,
            TotpService totpService,
            TenantSecuritySettingsService settingsService,
            SuspendedTenantRegistry suspendedTenantRegistry,
            ApplicationEventPublisher events) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.totpService = totpService;
        this.settingsService = settingsService;
        this.suspendedTenantRegistry = suspendedTenantRegistry;
        this.events = events;
    }

    public record LoginResult(
            boolean mfaRequired, String mfaChallengeToken, String accessToken, String refreshToken, boolean passwordChangeRequired) {

        static LoginResult mfaChallenge(String token) {
            return new LoginResult(true, token, null, null, false);
        }

        static LoginResult success(String accessToken, String refreshToken, boolean passwordChangeRequired) {
            return new LoginResult(false, null, accessToken, refreshToken, passwordChangeRequired);
        }
    }

    @Transactional
    public LoginResult login(UUID tenantId, String email, String rawPassword, String ipAddress, String userAgent) {
        TenantContext.set(tenantId);
        try {
            suspendedTenantRegistry.requireActive(tenantId);
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                if (user != null) {
                    user.recordFailedLogin(MAX_FAILED_LOGIN_ATTEMPTS, LOCKOUT_DURATION);
                    userRepository.save(user);
                }
                events.publishEvent(new AuthAuditEvents.LoginFailed(tenantId, email, "bad_credentials", ipAddress, Instant.now()));
                throw new AuthenticationFailedException("Invalid email or password");
            }
            if (user.isLocked()) {
                events.publishEvent(new AuthAuditEvents.LoginFailed(tenantId, email, "account_locked", ipAddress, Instant.now()));
                throw new AuthenticationFailedException("Account is temporarily locked");
            }

            user.recordSuccessfulLogin();
            userRepository.save(user);

            if (user.isTotpEnabled()) {
                return LoginResult.mfaChallenge(jwtService.issueMfaChallenge(user.getId(), tenantId));
            }
            return completeLogin(user, tenantId, ipAddress, userAgent);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public LoginResult verifyTotp(String mfaChallengeToken, String code, String ipAddress, String userAgent) {
        JwtService.AccessTokenClaims claims =
                jwtService.parseMfaChallenge(mfaChallengeToken).orElseThrow(() -> new AuthenticationFailedException("Invalid or expired challenge"));
        TenantContext.set(claims.tenantId());
        try {
            User user = userRepository.findById(claims.userId()).orElseThrow(() -> new AuthenticationFailedException("Invalid or expired challenge"));
            if (!user.isTotpEnabled() || !totpService.verify(user.getTotpSecret(), code)) {
                events.publishEvent(
                        new AuthAuditEvents.LoginFailed(claims.tenantId(), user.getEmail(), "bad_totp", ipAddress, Instant.now()));
                throw new AuthenticationFailedException("Invalid code");
            }
            return completeLogin(user, claims.tenantId(), ipAddress, userAgent);
        } finally {
            TenantContext.clear();
        }
    }

    private LoginResult completeLogin(User user, UUID tenantId, String ipAddress, String userAgent) {
        SecurityPolicy policy = settingsService.resolve(tenantId);
        String accessToken = jwtService.issueAccessToken(user.getId(), tenantId);
        String refreshToken = sessionService
                .issue(tenantId, user.getId(), ipAddress, userAgent, Duration.ofDays(7))
                .rawRefreshToken();
        boolean passwordChangeRequired =
                user.isMustChangePassword()
                        || (policy.expiryDays() != null
                                && user.getPasswordChangedAt().plusSeconds(policy.expiryDays() * 86400L).isBefore(Instant.now()));
        events.publishEvent(new AuthAuditEvents.LoginSucceeded(tenantId, user.getId(), ipAddress, Instant.now()));
        return LoginResult.success(accessToken, refreshToken, passwordChangeRequired);
    }

    public record TokenPair(String accessToken, String refreshToken) {}

    @Transactional
    public TokenPair refresh(String rawRefreshToken, String ipAddress, String userAgent) {
        UUID tenantId = SessionService.extractTenantId(rawRefreshToken);
        TenantContext.set(tenantId);
        try {
            suspendedTenantRegistry.requireActive(tenantId);
            SessionService.RotationResult rotation = sessionService.rotate(rawRefreshToken, ipAddress, userAgent, Duration.ofDays(7));
            String accessToken = jwtService.issueAccessToken(rotation.userId(), rotation.tenantId());
            return new TokenPair(accessToken, rotation.issued().rawRefreshToken());
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        UUID tenantId = SessionService.extractTenantId(rawRefreshToken);
        TenantContext.set(tenantId);
        try {
            sessionService.revokeByToken(rawRefreshToken);
        } finally {
            TenantContext.clear();
        }
    }
}
