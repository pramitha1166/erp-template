package com.eudext.erp.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eudext.erp.admin.internal.support.PlatformBootstrapService;
import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.iam.AuthenticationApi;
import com.eudext.erp.iam.AuthenticationFailedException;
import com.eudext.erp.iam.IdentityProvisioningApi;
import com.eudext.erp.iam.internal.user.UserService;
import com.eudext.erp.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end coverage of 0.11.10: the one-shot platform-admin bootstrap
 * (ADM-1) and the admin realm's dedicated login entry point (ADM-1/ADM-5),
 * including the IAM-9 forced password rotation on the bootstrap credential.
 *
 * <p>Kept as a single test method: {@code PlatformBootstrapService}'s
 * "exactly one platform admin, ever" gate is global (keyed on the fixed
 * {@link PlatformIdentifiers#PLATFORM_COMPANY_ID} sentinel, not anything
 * per-test like a random tenant id), so a second {@code @Test} method
 * bootstrapping its own admin would collide with this one under the shared
 * Testcontainers Postgres {@link AbstractIntegrationTest} uses.
 */
class AdminAuthFlowIT extends AbstractIntegrationTest {

    @Autowired
    private PlatformBootstrapService bootstrapService;

    @Autowired
    private AuthenticationApi authenticationApi;

    @Autowired
    private UserService userService;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void bootstrapLoginForcedRotationAndOneShotGuard() {
        IdentityProvisioningApi.ProvisionedUser admin = bootstrapService.bootstrapFirstPlatformAdmin("root@eudext.test");

        assertThatThrownBy(() -> authenticationApi.login(
                        PlatformIdentifiers.PLATFORM_TENANT_ID, admin.email(), "NotTheRealPassword1!", "127.0.0.1", "junit"))
                .isInstanceOf(AuthenticationFailedException.class);

        AuthenticationApi.LoginOutcome firstLogin = authenticationApi.login(
                PlatformIdentifiers.PLATFORM_TENANT_ID, admin.email(), admin.temporaryPassword(), "127.0.0.1", "junit");
        assertThat(firstLogin.mfaRequired()).isFalse();
        assertThat(firstLogin.accessToken()).isNotBlank();
        assertThat(firstLogin.refreshToken()).isNotBlank();
        assertThat(firstLogin.passwordChangeRequired()).isTrue();

        TenantContext.set(PlatformIdentifiers.PLATFORM_TENANT_ID);
        userService.changePassword(
                PlatformIdentifiers.PLATFORM_TENANT_ID, admin.userId(), admin.temporaryPassword(), "N3wStrongerPassw0rd!");
        TenantContext.clear();

        AuthenticationApi.LoginOutcome secondLogin = authenticationApi.login(
                PlatformIdentifiers.PLATFORM_TENANT_ID, admin.email(), "N3wStrongerPassw0rd!", "127.0.0.1", "junit");
        assertThat(secondLogin.passwordChangeRequired()).isFalse();

        assertThatThrownBy(() -> bootstrapService.bootstrapFirstPlatformAdmin("second-admin@eudext.test"))
                .isInstanceOf(IllegalStateException.class);
    }
}
