package com.eudext.erp.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.eudext.erp.admin.internal.tenant.Tenant;
import com.eudext.erp.admin.internal.tenant.TenantOnboardingService;
import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.masterdata.MasterDataProvisioningApi;
import com.eudext.erp.testsupport.AbstractIntegrationTest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * ADM-2 / ADM-3: an end-to-end smoke test proving the full onboarding
 * orchestration actually wires together and produces correctly-scoped
 * rows against a real Postgres instance — not just that the mocked {@code
 * TenantOnboardingServiceTest} calls the right collaborators in the right
 * order. This runs through the application's own (Testcontainers
 * bootstrap-role) connection pool, which — like the admin connection in
 * {@code TenantIsolationIT} — bypasses RLS, so it does not by itself prove
 * the {@code TenantContextScope} mid-transaction re-stamp fix; {@code
 * TenantContextScopeRestampIT} is the direct regression test for that
 * mechanism, isolated from the rest of this orchestration.
 */
class TenantOnboardingIT extends AbstractIntegrationTest {

    @Autowired
    private TenantOnboardingService onboardingService;

    @Autowired
    private Environment environment;

    private final UUID brandId = UUID.randomUUID();
    private final UUID callerAdminTenantId = UUID.randomUUID();

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void onboardingWritesEveryTenantScopedRowUnderTheNewTenantsOwnRlsScope() throws SQLException {
        insertBrandRow(brandId);

        // Simulates a platform/brand admin's own ambient tenant context —
        // deliberately NOT the tenant being onboarded, and deliberately
        // set before the transactional call so the very first statement
        // inside it (the tenants-table insert) checks out and stamps the
        // connection under THIS tenant, exactly like a real admin request.
        TenantContext.set(callerAdminTenantId);

        var company = new MasterDataProvisioningApi.NewCompany("Acme Lanka Pvt Ltd", "REG-1", "VAT-1", "Colombo", "LKR", 1);
        var request = new TenantOnboardingService.Request(
                "Acme Lanka", company, "admin+" + UUID.randomUUID() + "@acme.test", Set.of());

        Tenant tenant = onboardingService.onboard(brandId, request, "test-actor");
        TenantContext.clear();

        // Confirms every seeded row landed with the new tenant's id and
        // the checklist's exact fixed item count — the orchestration's
        // correctness, not RLS enforcement itself (see this class's
        // javadoc for why: this connection is the bootstrap superuser
        // role and bypasses RLS regardless).
        try (Connection connection = tenantScopedConnection(tenant.getId())) {
            assertThat(countWhereTenant(connection, "companies", tenant.getId())).isEqualTo(1);
            assertThat(countWhereTenant(connection, "accounts", tenant.getId())).isGreaterThan(0);
            assertThat(countWhereTenant(connection, "fiscal_years", tenant.getId())).isEqualTo(1);
            assertThat(countWhereTenant(connection, "numbering_series", tenant.getId())).isGreaterThan(0);
            assertThat(countWhereTenant(connection, "onboarding_checklist_items", tenant.getId())).isEqualTo(6);
        }

        // And that none of it leaked into the caller admin's own tenant scope.
        try (Connection connection = tenantScopedConnection(callerAdminTenantId)) {
            assertThat(countWhereTenant(connection, "companies", tenant.getId())).isZero();
        }
    }

    private void insertBrandRow(UUID brandId) throws SQLException {
        try (Connection connection = adminConnection();
                PreparedStatement insert = connection.prepareStatement("INSERT INTO brands (id, name, status) VALUES (?, ?, 'ACTIVE')")) {
            insert.setObject(1, brandId);
            insert.setString(2, "Test Brand");
            insert.executeUpdate();
        }
    }

    private Connection adminConnection() throws SQLException {
        return DriverManager.getConnection(
                environment.getProperty("spring.datasource.url"),
                environment.getProperty("spring.datasource.username"),
                environment.getProperty("spring.datasource.password"));
    }

    private Connection tenantScopedConnection(UUID tenantId) throws SQLException {
        Connection connection = adminConnection();
        try (PreparedStatement statement = connection.prepareStatement("SELECT set_config('app.tenant_id', ?, false)")) {
            statement.setString(1, tenantId.toString());
            statement.execute();
        }
        return connection;
    }

    private int countWhereTenant(Connection connection, String table, UUID tenantId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT count(*) FROM " + table + " WHERE tenant_id = ?")) {
            statement.setObject(1, tenantId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
