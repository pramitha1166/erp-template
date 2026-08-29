package com.eudext.erp.admin.internal.web;

import com.eudext.erp.admin.PlatformIdentifiers;
import com.eudext.erp.iam.AuthenticationApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADM-1 / ADM-5: the admin realm's own login entry point — platform and
 * brand admin staff are ordinary {@code iam.User} rows homed under {@link
 * PlatformIdentifiers#PLATFORM_TENANT_ID} (see that class's javadoc), but
 * unlike a tenant user they have no real tenant to supply, so this never
 * accepts a caller-supplied {@code tenantId} the way {@code /auth/login}
 * does — the sentinel is fixed here, server-side, once. Reuses {@code
 * /auth/totp/verify}, {@code /auth/refresh}, and {@code /auth/logout} as-is:
 * none of those take a tenant id from the caller, so they already work
 * unchanged for a session issued through this endpoint (the refresh token
 * carries its own tenant, and the MFA challenge token carries its own).
 */
@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final AuthenticationApi authenticationApi;

    public AdminAuthController(AuthenticationApi authenticationApi) {
        this.authenticationApi = authenticationApi;
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record LoginResponse(
            boolean mfaRequired,
            String mfaChallengeToken,
            String accessToken,
            String refreshToken,
            boolean passwordChangeRequired) {}

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthenticationApi.LoginOutcome outcome = authenticationApi.login(
                PlatformIdentifiers.PLATFORM_TENANT_ID,
                request.email(),
                request.password(),
                clientIp(httpRequest),
                userAgent(httpRequest));
        return new LoginResponse(
                outcome.mfaRequired(),
                outcome.mfaChallengeToken(),
                outcome.accessToken(),
                outcome.refreshToken(),
                outcome.passwordChangeRequired());
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor != null ? forwardedFor.split(",")[0].trim() : request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
