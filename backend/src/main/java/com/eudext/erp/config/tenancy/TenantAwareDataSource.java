package com.eudext.erp.config.tenancy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * ARCH-2: stamps every JDBC connection handed out with the current
 * {@link TenantContext} as the Postgres session variable {@code
 * app.tenant_id}, which every RLS policy (see {@code current_tenant_id()}
 * in {@code V2__tenant_isolation_support.sql}) filters on.
 *
 * <p>Uses session-level {@code set_config(..., false)}, not {@code
 * SET LOCAL}: Hikari hands out one physical connection per logical
 * checkout (one per transaction under Spring's default connection release
 * mode), so stamping at checkout time — here, in {@link #getConnection()}
 * — covers exactly one transaction's lifetime. Every checkout re-stamps
 * the value (including to empty when no tenant is set), so a connection
 * returning to the pool can never leak a stale tenant into the next
 * borrower.
 *
 * <p>Because stamping only happens at checkout, a plain {@code
 * TenantContext.set} partway through an already-running transaction has
 * no effect on the connection that transaction is already holding — see
 * {@link TenantContextScope}, which re-stamps the live connection via
 * {@link #stamp} for exactly that case (a platform/brand-admin operation
 * that needs to act within a specific tenant's RLS scope for one step of
 * an otherwise admin-scoped transaction).
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    private static final String SET_TENANT_SQL = "SELECT set_config('app.tenant_id', ?, false)";

    public TenantAwareDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return stampTenant(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return stampTenant(super.getConnection(username, password));
    }

    private Connection stampTenant(Connection connection) throws SQLException {
        stamp(connection, TenantContext.get().orElse(null));
        return connection;
    }

    /**
     * ADM-1 / ADM-5 / ADM-9: re-stamps an already-checked-out connection.
     * Needed by {@link TenantContextScope}, which switches {@link
     * TenantContext} mid-transaction for a cross-tenant admin
     * operation — a plain {@code TenantContext.set} has no effect on a
     * connection that's already bound to the current transaction, since
     * this class only stamps at checkout time (see this class's own
     * javadoc), and a mid-transaction {@code TenantContextScope.enter}
     * runs after checkout, not before it.
     */
    static void stamp(Connection connection, UUID tenantId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SET_TENANT_SQL)) {
            statement.setString(1, tenantId == null ? "" : tenantId.toString());
            statement.execute();
        }
    }
}
