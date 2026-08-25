package com.eudext.erp.audit.internal.authevents;

import com.eudext.erp.audit.internal.log.AuditAction;
import com.eudext.erp.audit.internal.write.AuditLogWriter;
import com.eudext.erp.iam.AuthAuditEvents;
import java.util.Map;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * IAM-10 / AUD-1: turns the auth-domain events IAM publishes (see the
 * javadoc on {@link AuthAuditEvents}) into audit_log rows. These events
 * don't correspond one-to-one to a single entity mutation the generic
 * {@code AuditingInterceptor} would see — a failed login touches no row at
 * all, and a successful one spans a user update plus a new session insert —
 * so IAM publishes them explicitly instead of relying on the interceptor.
 *
 * <p>{@code @ApplicationModuleListener} runs each handler after the
 * publishing transaction commits, exactly like the interceptor's own
 * post-commit write, so a rolled-back login attempt never gets audited.
 */
@Component
class AuthAuditEventListener {

    private final AuditLogWriter writer;

    AuthAuditEventListener(AuditLogWriter writer) {
        this.writer = writer;
    }

    @ApplicationModuleListener
    void on(AuthAuditEvents.LoginSucceeded event) {
        writer.write(
                event.tenantId(),
                "AuthEvent",
                event.userId().toString(),
                AuditAction.INSERT,
                Map.of(),
                Map.of("event", "LOGIN_SUCCEEDED"),
                event.userId().toString(),
                event.ipAddress(),
                null,
                event.occurredAt());
    }

    @ApplicationModuleListener
    void on(AuthAuditEvents.LoginFailed event) {
        writer.write(
                event.tenantId(),
                "AuthEvent",
                event.email(),
                AuditAction.INSERT,
                Map.of(),
                Map.of("event", "LOGIN_FAILED", "reason", event.reason()),
                event.email(),
                event.ipAddress(),
                null,
                event.occurredAt());
    }

    @ApplicationModuleListener
    void on(AuthAuditEvents.SessionRevoked event) {
        writer.write(
                event.tenantId(),
                "UserSession",
                event.sessionId().toString(),
                AuditAction.UPDATE,
                Map.of(),
                Map.of("event", "SESSION_REVOKED", "reason", event.reason()),
                event.userId().toString(),
                null,
                null,
                event.occurredAt());
    }

    @ApplicationModuleListener
    void on(AuthAuditEvents.PermissionGranted event) {
        writer.write(
                event.tenantId(),
                "Role",
                event.roleId().toString(),
                AuditAction.UPDATE,
                Map.of(),
                Map.of("event", "PERMISSION_GRANTED", "permissionCode", event.permissionCode()),
                event.grantedBy(),
                null,
                null,
                event.occurredAt());
    }

    @ApplicationModuleListener
    void on(AuthAuditEvents.PermissionRevoked event) {
        writer.write(
                event.tenantId(),
                "Role",
                event.roleId().toString(),
                AuditAction.UPDATE,
                Map.of("permissionCode", event.permissionCode()),
                Map.of("event", "PERMISSION_REVOKED"),
                event.revokedBy(),
                null,
                null,
                event.occurredAt());
    }

    @ApplicationModuleListener
    void on(AuthAuditEvents.RoleAssigned event) {
        writer.write(
                event.tenantId(),
                "UserCompanyRole",
                event.userId() + ":" + event.companyId() + ":" + event.roleId(),
                AuditAction.INSERT,
                Map.of(),
                Map.of("event", "ROLE_ASSIGNED", "userId", event.userId(), "companyId", event.companyId(), "roleId", event.roleId()),
                event.assignedBy(),
                null,
                null,
                event.occurredAt());
    }

    @ApplicationModuleListener
    void on(AuthAuditEvents.RoleUnassigned event) {
        writer.write(
                event.tenantId(),
                "UserCompanyRole",
                event.userId() + ":" + event.companyId() + ":" + event.roleId(),
                AuditAction.DELETE,
                Map.of("userId", event.userId(), "companyId", event.companyId(), "roleId", event.roleId()),
                Map.of(),
                event.unassignedBy(),
                null,
                null,
                event.occurredAt());
    }
}
