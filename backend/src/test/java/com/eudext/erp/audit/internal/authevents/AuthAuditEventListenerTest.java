package com.eudext.erp.audit.internal.authevents;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.eudext.erp.audit.internal.log.AuditAction;
import com.eudext.erp.audit.internal.write.AuditLogWriter;
import com.eudext.erp.iam.AuthAuditEvents;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Exercises the listener methods directly rather than through Spring's
 * event bus: {@code @ApplicationModuleListener} only fires after a real,
 * committed Spring-managed transaction (see its javadoc), which a plain
 * unit test doesn't have — and isn't the thing worth re-verifying here
 * anyway, that's Spring Modulith's own tested behavior. What's worth
 * testing is that each event type is translated into the right audit_log
 * write.
 */
@ExtendWith(MockitoExtension.class)
class AuthAuditEventListenerTest {

    @Mock
    private AuditLogWriter writer;

    private AuthAuditEventListener listener;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        listener = new AuthAuditEventListener(writer);
    }

    @Test
    void loginSucceededIsAuditedAsAnInsertActedByTheUser() {
        listener.on(new AuthAuditEvents.LoginSucceeded(tenantId, userId, "10.0.0.1", now));

        verify(writer)
                .write(
                        eq(tenantId),
                        eq("AuthEvent"),
                        eq(userId.toString()),
                        eq(AuditAction.INSERT),
                        anyMap(),
                        anyMap(),
                        eq(userId.toString()),
                        eq("10.0.0.1"),
                        any(),
                        eq(now));
    }

    @Test
    void loginFailedIsAuditedKeyedByEmailSinceThereIsNoUserIdYet() {
        listener.on(new AuthAuditEvents.LoginFailed(tenantId, "someone@example.com", "bad_credentials", "10.0.0.1", now));

        verify(writer)
                .write(
                        eq(tenantId),
                        eq("AuthEvent"),
                        eq("someone@example.com"),
                        eq(AuditAction.INSERT),
                        anyMap(),
                        anyMap(),
                        eq("someone@example.com"),
                        eq("10.0.0.1"),
                        any(),
                        eq(now));
    }

    @Test
    void permissionRevokedRecordsTheRevokedCodeAsTheOldValue() {
        UUID roleId = UUID.randomUUID();

        listener.on(new AuthAuditEvents.PermissionRevoked(tenantId, roleId, "finance:payment:approve", "admin-user", now));

        verify(writer)
                .write(
                        eq(tenantId),
                        eq("Role"),
                        eq(roleId.toString()),
                        eq(AuditAction.UPDATE),
                        eq(java.util.Map.of("permissionCode", "finance:payment:approve")),
                        anyMap(),
                        eq("admin-user"),
                        any(),
                        any(),
                        eq(now));
    }

    @Test
    void roleUnassignedIsAuditedAsADeleteOfTheAssignmentTriple() {
        UUID companyId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        listener.on(new AuthAuditEvents.RoleUnassigned(tenantId, userId, companyId, roleId, "admin-user", now));

        verify(writer)
                .write(
                        eq(tenantId),
                        eq("UserCompanyRole"),
                        eq(userId + ":" + companyId + ":" + roleId),
                        eq(AuditAction.DELETE),
                        anyMap(),
                        anyMap(),
                        eq("admin-user"),
                        any(),
                        any(),
                        eq(now));
    }
}
