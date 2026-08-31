package com.eudext.erp.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.iam.internal.auth.AuthService;
import com.eudext.erp.iam.internal.rbac.Role;
import com.eudext.erp.iam.internal.rbac.RoleService;
import com.eudext.erp.iam.internal.rbac.TotpRequiredException;
import com.eudext.erp.iam.internal.rbac.UserRoleAssignmentService;
import com.eudext.erp.iam.internal.session.RefreshTokenReuseDetectedException;
import com.eudext.erp.iam.internal.session.SessionService;
import com.eudext.erp.iam.internal.sod.SegregationOfDutiesService;
import com.eudext.erp.iam.internal.sod.SegregationOfDutiesViolationException;
import com.eudext.erp.iam.internal.totp.TotpEnrollmentService;
import com.eudext.erp.iam.internal.totp.TotpService;
import com.eudext.erp.iam.internal.user.User;
import com.eudext.erp.iam.internal.user.UserRepository;
import com.eudext.erp.iam.internal.user.UserService;
import com.eudext.erp.testsupport.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end coverage of the IAM epic against a real Postgres: login,
 * refresh rotation with reuse detection (IAM-1), RBAC + tenant-scoped user
 * lookup under RLS (IAM-3, and NFR-S6 for the `users` table specifically),
 * Segregation-of-Duties blocking (IAM-7), and TOTP-mandatory-for-approval
 * enforcement (IAM-2).
 */
class AuthFlowIT extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleAssignmentService userRoleAssignmentService;

    @Autowired
    private SegregationOfDutiesService segregationOfDutiesService;

    @Autowired
    private TotpEnrollmentService totpEnrollmentService;

    @Autowired
    private TotpService totpService;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void loginThenRefreshThenReusePresentedAgainIsDetected() {
        UUID tenantId = UUID.randomUUID();
        String password = "Str0ngPassw0rd!";
        createUser(tenantId, "alice@example.com", password);

        AuthService.LoginResult login = authService.login(tenantId, "alice@example.com", password, "127.0.0.1", "junit");
        assertThat(login.mfaRequired()).isFalse();
        assertThat(login.accessToken()).isNotBlank();
        assertThat(login.refreshToken()).isNotBlank();

        AuthService.TokenPair refreshed = authService.refresh(login.refreshToken(), "127.0.0.1", "junit");
        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotEqualTo(login.refreshToken());

        // The original refresh token was rotated away by the call above; presenting it again is theft-shaped reuse.
        assertThatThrownBy(() -> authService.refresh(login.refreshToken(), "10.0.0.1", "attacker"))
                .isInstanceOf(RefreshTokenReuseDetectedException.class);
    }

    @Test
    void wrongPasswordIsRejectedWithoutRevealingWhichPartWasWrong() {
        UUID tenantId = UUID.randomUUID();
        createUser(tenantId, "bob@example.com", "Str0ngPassw0rd!");

        assertThatThrownBy(() -> authService.login(tenantId, "bob@example.com", "WrongPassword1", "127.0.0.1", "junit"))
                .isInstanceOf(com.eudext.erp.iam.AuthenticationFailedException.class);
        assertThatThrownBy(() -> authService.login(tenantId, "nobody@example.com", "WrongPassword1", "127.0.0.1", "junit"))
                .isInstanceOf(com.eudext.erp.iam.AuthenticationFailedException.class);
    }

    @Test
    void aUserInOneTenantIsInvisibleToAnotherTenantsLookup() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        createUser(tenantA, "shared-email@example.com", "Str0ngPassw0rd!");

        // Same email is free to reuse in a different tenant — uniqueness is scoped per tenant (V4 migration).
        TenantContext.set(tenantB);
        User userInB = userService.createUser(tenantB, "shared-email@example.com", "Str0ngPassw0rd!");
        assertThat(userInB).isNotNull();

        TenantContext.set(tenantA);
        assertThat(userRepository.findByEmail("shared-email@example.com")).isPresent();

        TenantContext.set(tenantB);
        assertThat(userRepository.findByEmail("shared-email@example.com")).isPresent();

        TenantContext.clear();
        assertThat(userRepository.findByEmail("shared-email@example.com")).isEmpty();
    }

    @Test
    void grantingConflictingPermissionsOnTheSameRoleIsBlockedBySoD() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);

        segregationOfDutiesService.createRule(
                tenantId, "procurement:supplier:create", "finance:payment:approve", "classic SoD conflict");
        Role role = roleService.createRole(tenantId, "Rogue Role", null);
        roleService.grantPermission(tenantId, role.getId(), "procurement:supplier:create", "test");

        assertThatThrownBy(() -> roleService.grantPermission(tenantId, role.getId(), "finance:payment:approve", "test"))
                .isInstanceOf(SegregationOfDutiesViolationException.class);
    }

    @Test
    void assigningARoleThatWouldCreateACrossRoleConflictIsBlocked() {
        UUID tenantId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        TenantContext.set(tenantId);

        segregationOfDutiesService.createRule(
                tenantId, "procurement:supplier:create", "finance:payment:approve", "classic SoD conflict");
        Role creatorRole = roleService.createRole(tenantId, "Supplier Creator", null);
        roleService.grantPermission(tenantId, creatorRole.getId(), "procurement:supplier:create", "test");
        Role approverRole = roleService.createRole(tenantId, "Payment Approver", null);
        roleService.grantPermission(tenantId, approverRole.getId(), "finance:payment:approve", "test");

        User user = userService.createUser(tenantId, "carol@example.com", "Str0ngPassw0rd!");
        userRoleAssignmentService.assign(tenantId, user.getId(), companyId, creatorRole.getId(), "test");

        assertThatThrownBy(() -> userRoleAssignmentService.assign(tenantId, user.getId(), companyId, approverRole.getId(), "test"))
                .isInstanceOf(SegregationOfDutiesViolationException.class);
    }

    @Test
    void assigningAnApprovalRoleRequiresTotpToAlreadyBeEnabled() {
        UUID tenantId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        TenantContext.set(tenantId);

        Role approverRole = roleService.createRole(tenantId, "Payment Approver", null);
        roleService.grantPermission(tenantId, approverRole.getId(), "finance:payment:approve", "test");
        User user = userService.createUser(tenantId, "dave@example.com", "Str0ngPassw0rd!");

        assertThatThrownBy(() -> userRoleAssignmentService.assign(tenantId, user.getId(), companyId, approverRole.getId(), "test"))
                .isInstanceOf(TotpRequiredException.class);

        var enrollment = totpEnrollmentService.beginEnrollment(user.getId());
        String code = currentCode(enrollment.secret());
        totpEnrollmentService.confirmEnrollment(user.getId(), code);

        userRoleAssignmentService.assign(tenantId, user.getId(), companyId, approverRole.getId(), "test");
    }

    private void createUser(UUID tenantId, String email, String password) {
        TenantContext.set(tenantId);
        userService.createUser(tenantId, email, password);
        TenantContext.clear();
    }

    /**
     * Derives the current code for a freshly-generated secret via a
     * reflective call into {@code TotpService}'s private step generator —
     * the RFC 6238 math itself is exercised directly (not reflectively)
     * in {@code TotpServiceTest}; this just needs *a* valid code to prove
     * the enrollment/enforcement wiring around it works end to end.
     */
    private String currentCode(String base32Secret) {
        try {
            var method = TotpService.class.getDeclaredMethod("generateCode", String.class, long.class);
            method.setAccessible(true);
            long step = java.time.Instant.now().getEpochSecond() / 30;
            return (String) method.invoke(totpService, base32Secret, step);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
