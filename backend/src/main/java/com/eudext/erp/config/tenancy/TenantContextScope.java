package com.eudext.erp.config.tenancy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * ADM-1 / ADM-5 / ADM-9: lets a platform- or brand-admin operation act
 * within a single target tenant's RLS scope for the duration of one call,
 * then restores whatever tenant (if any) was ambient before it. Platform
 * and brand admin staff authenticate against a sentinel "home" tenant (see
 * {@code com.eudext.erp.admin.PlatformIdentifiers}), not the tenant they are
 * administering, so a plain {@link TenantContext#set} would leave every
 * tenant-scoped repository call seeing zero rows under RLS. Cross-tenant
 * admin reads/writes wrap the single scoped call in this instead of
 * granting any blanket RLS bypass — each switch is short-lived, explicit at
 * the call site, and only ever entered after the caller's admin permission
 * has already been checked in Java.
 *
 * <p>Not a general-purpose "impersonate any tenant" backdoor: this changes
 * only which tenant's rows a query can see, never which user is acting
 * (that's {@link ImpersonationContext}, used for actual impersonated
 * sessions) — so audit entries written during a scope still attribute the
 * change to the real admin's own identity.
 *
 * <p><b>Why this also re-stamps the live connection:</b> {@link
 * TenantAwareDataSource} only stamps {@code app.tenant_id} when a
 * connection is checked out, which happens once per transaction (see its
 * javadoc) — a bare {@code TenantContext.set} partway through an
 * already-open transaction (e.g. after an earlier statement in the same
 * {@code @Transactional} method ran under the admin's own ambient tenant)
 * would leave the live connection's session variable unchanged, and every
 * RLS {@code WITH CHECK} after it would fail. {@link #enter} and {@link
 * #close} both re-run the stamp against whatever connection is currently
 * bound to the active transaction (a no-op if none is active yet — the
 * next checkout will pick up the new value naturally).
 */
public final class TenantContextScope implements AutoCloseable {

    private static volatile DataSource dataSource;

    private final UUID previousTenantId;

    private TenantContextScope(UUID previousTenantId) {
        this.previousTenantId = previousTenantId;
    }

    /** Called once at startup by {@code TenantContextScopeInitializer} with the tenant-aware {@code DataSource} bean. */
    static void bind(DataSource ds) {
        dataSource = ds;
    }

    public static TenantContextScope enter(UUID tenantId) {
        UUID previous = TenantContext.get().orElse(null);
        TenantContext.set(tenantId);
        restampActiveConnection();
        return new TenantContextScope(previous);
    }

    @Override
    public void close() {
        TenantContext.set(previousTenantId);
        restampActiveConnection();
    }

    private static void restampActiveConnection() {
        DataSource ds = dataSource;
        if (ds == null || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        Connection connection = DataSourceUtils.getConnection(ds);
        try {
            TenantAwareDataSource.stamp(connection, TenantContext.get().orElse(null));
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to switch tenant context mid-transaction", e);
        } finally {
            DataSourceUtils.releaseConnection(connection, ds);
        }
    }
}
