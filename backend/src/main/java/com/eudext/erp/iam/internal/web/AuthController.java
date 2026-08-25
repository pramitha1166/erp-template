package com.eudext.erp.iam.internal.web;

import com.eudext.erp.iam.internal.auth.AuthService;
import com.eudext.erp.iam.internal.auth.CurrentUserResolver;
import com.eudext.erp.iam.internal.session.SessionService;
import com.eudext.erp.iam.internal.session.UserSession;
import com.eudext.erp.iam.internal.totp.TotpEnrollmentService;
import com.eudext.erp.iam.internal.user.UserService;
import com.eudext.erp.config.tenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** IAM-1 / IAM-2 / IAM-8: login, refresh, logout, TOTP enrollment, and the caller's own session list. */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;
    private final TotpEnrollmentService totpEnrollmentService;
    private final UserService userService;
    private final CurrentUserResolver currentUserResolver;

    public AuthController(
            AuthService authService,
            SessionService sessionService,
            TotpEnrollmentService totpEnrollmentService,
            UserService userService,
            CurrentUserResolver currentUserResolver) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.totpEnrollmentService = totpEnrollmentService;
        this.userService = userService;
        this.currentUserResolver = currentUserResolver;
    }

    public record LoginRequest(UUID tenantId, @Email @NotBlank String email, @NotBlank String password) {}

    public record LoginResponse(
            boolean mfaRequired,
            String mfaChallengeToken,
            String accessToken,
            String refreshToken,
            boolean passwordChangeRequired) {}

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthService.LoginResult result = authService.login(
                request.tenantId(), request.email(), request.password(), clientIp(httpRequest), userAgent(httpRequest));
        return new LoginResponse(
                result.mfaRequired(),
                result.mfaChallengeToken(),
                result.accessToken(),
                result.refreshToken(),
                result.passwordChangeRequired());
    }

    public record TotpVerifyRequest(@NotBlank String mfaChallengeToken, @NotBlank String code) {}

    @PostMapping("/totp/verify")
    public LoginResponse verifyTotp(@Valid @RequestBody TotpVerifyRequest request, HttpServletRequest httpRequest) {
        AuthService.LoginResult result = authService.verifyTotp(
                request.mfaChallengeToken(), request.code(), clientIp(httpRequest), userAgent(httpRequest));
        return new LoginResponse(
                result.mfaRequired(),
                result.mfaChallengeToken(),
                result.accessToken(),
                result.refreshToken(),
                result.passwordChangeRequired());
    }

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record RefreshResponse(String accessToken, String refreshToken) {}

    @PostMapping("/refresh")
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        AuthService.TokenPair pair = authService.refresh(request.refreshToken(), clientIp(httpRequest), userAgent(httpRequest));
        return new RefreshResponse(pair.accessToken(), pair.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    public record TotpSetupResponse(String secret, String otpAuthUri) {}

    @PostMapping("/totp/setup")
    public TotpSetupResponse setupTotp() {
        var start = totpEnrollmentService.beginEnrollment(currentUserResolver.currentUserId());
        return new TotpSetupResponse(start.secret(), start.otpAuthUri());
    }

    public record TotpEnableRequest(@NotBlank String code) {}

    @PostMapping("/totp/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enableTotp(@Valid @RequestBody TotpEnableRequest request) {
        totpEnrollmentService.confirmEnrollment(currentUserResolver.currentUserId(), request.code());
    }

    @PostMapping("/totp/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableTotp() {
        totpEnrollmentService.disable(currentUserResolver.currentUserId());
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}

    // TenantContext is already set for this request by JwtAuthenticationFilter, so tenantId comes from there.
    @PostMapping("/password/change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UUID tenantId = TenantContext.get()
                .orElseThrow(() -> new IllegalStateException("No tenant context on an authenticated request"));
        userService.changePassword(tenantId, currentUserResolver.currentUserId(), request.currentPassword(), request.newPassword());
    }

    public record SessionView(UUID id, Instant issuedAt, Instant lastSeenAt, String ipAddress, String userAgent) {}

    @GetMapping("/sessions")
    public List<SessionView> listSessions() {
        return sessionService.listActive(currentUserResolver.currentUserId()).stream()
                .map(s -> new SessionView(s.getId(), s.getIssuedAt(), s.getLastSeenAt(), s.getIpAddress(), s.getUserAgent()))
                .toList();
    }

    /** IAM-8: force logout — a user may only revoke their own sessions here; admin-initiated force-logout is a Phase 1+ extension. */
    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSession(@PathVariable UUID sessionId) {
        UUID userId = currentUserResolver.currentUserId();
        boolean ownsSession = sessionService.listActive(userId).stream().map(UserSession::getId).anyMatch(sessionId::equals);
        if (!ownsSession) {
            throw new AccessDeniedException("Not your session");
        }
        sessionService.revoke(sessionId);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor != null ? forwardedFor.split(",")[0].trim() : request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
