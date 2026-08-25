package com.eudext.erp.config.tenancy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
        String tenantId = TenantContext.get().map(Object::toString).orElse("");
        try (PreparedStatement statement = connection.prepareStatement(SET_TENANT_SQL)) {
            statement.setString(1, tenantId);
            statement.execute();
        }
        return connection;
    }
}
