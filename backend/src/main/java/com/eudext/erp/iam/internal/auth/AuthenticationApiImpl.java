package com.eudext.erp.iam.internal.auth;

import com.eudext.erp.iam.AuthenticationApi;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class AuthenticationApiImpl implements AuthenticationApi {

    private final AuthService authService;

    AuthenticationApiImpl(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public LoginOutcome login(UUID tenantId, String email, String rawPassword, String ipAddress, String userAgent) {
        AuthService.LoginResult result = authService.login(tenantId, email, rawPassword, ipAddress, userAgent);
        return new LoginOutcome(
                result.mfaRequired(),
                result.mfaChallengeToken(),
                result.accessToken(),
                result.refreshToken(),
                result.passwordChangeRequired());
    }
}
