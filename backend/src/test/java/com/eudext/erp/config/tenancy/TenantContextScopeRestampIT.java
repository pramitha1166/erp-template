package com.eudext.erp.config.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eudext.erp.testsupport.AbstractIntegrationTest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * ADM-1 / ADM-5 / ADM-9: the direct regression test for the bug {@link
 * TenantContextScope} exists to fix — {@link TenantAwareDataSource} only
 * stamps {@code app.tenant_id} at connection checkout (once per
 * transaction), so a bare {@code TenantContext.set} partway through an
 * already-open transaction has zero effect on the live connection, and any
 * RLS-protected write after it would be rejected by the {@code WITH CHECK}
 * clause. This proves {@link TenantAwareDataSource#stamp} — the mechanism
 * {@link TenantContextScope#enter}/{@link TenantContextScope#close} use —
 * actually flips which tenant a write is accepted for, on one already-open
 * connection, without a new checkout.
 *
 * <p>Runs through a dedicated {@code NOBYPASSRLS} role rather than the
 * Testcontainers bootstrap user for the same reason {@code
 * TenantIsolationIT} does: a superuser connection would let every
 * assertion here pass whether or not the stamp actually changed anything.
 */
class TenantContextScopeRestampIT extends AbstractIntegrationTest {

    private static final String RESTRICTED_ROLE = "tenant_scope_restamp_test_role";
    private static final String RESTRICTED_PASSWORD = "tenant-scope-restamp-test";

    @Autowired
    private Environment environment;

    @Test
    void restampingALiveConnectionChangesWhichTenantsWritesAreAccepted() throws SQLException {
        provisionRestrictedRole();
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        try (Connection connection = restrictedConnection()) {
            // Simulates connection checkout at the start of an admin
            // transaction, under the admin's own ambient tenant (tenant A)
            // — exactly what happens before TenantOnboardingService.onboard
            // writes its first (unscoped, RLS-exempt) `tenants` row.
            TenantAwareDataSource.stamp(connection, tenantA);

            // Before any restamp, a write claiming tenant B — same shape
            // as everything TenantOnboardingService writes once it enters
            // TenantContextScope — is rejected: this is the bug, caught in
            // the act.
            assertThatThrownBy(() -> insertDummyDocument(connection, tenantB)).isInstanceOf(SQLException.class);

            // TenantContextScope.enter/close call exactly this against the
            // transaction's live connection.
            TenantAwareDataSource.stamp(connection, tenantB);

            // The identical write now succeeds, on the SAME connection —
            // proving the fix works without a fresh checkout.
            UUID insertedId = insertDummyDocument(connection, tenantB);
            assertThat(visibleDocumentIds(connection)).contains(insertedId);

            // And restoring to tenant A (what `close()` does) locks tenant B's row out again.
            TenantAwareDataSource.stamp(connection, tenantA);
            assertThat(visibleDocumentIds(connection)).doesNotContain(insertedId);
        }
    }

    private UUID insertDummyDocument(Connection connection, UUID tenantId) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO dummy_documents (id, tenant_id, company_id, branch_id, doc_status, posting_date) "
                        + "VALUES (?, ?, ?, ?, 'DRAFT', CURRENT_DATE)")) {
            insert.setObject(1, id);
            insert.setObject(2, tenantId);
            insert.setObject(3, UUID.randomUUID());
            insert.setObject(4, UUID.randomUUID());
            insert.executeUpdate();
        }
        return id;
    }

    private java.util.List<UUID> visibleDocumentIds(Connection connection) throws SQLException {
        java.util.List<UUID> ids = new java.util.ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM dummy_documents");
                var resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add((UUID) resultSet.getObject("id"));
            }
        }
        return ids;
    }

    private void provisionRestrictedRole() throws SQLException {
        try (Connection admin = adminConnection();
                Statement statement = admin.createStatement()) {
            statement.execute(
                    """
                    DO $$
                    BEGIN
                        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '%s') THEN
                            EXECUTE 'DROP OWNED BY %s';
                            EXECUTE 'DROP ROLE %s';
                        END IF;
                    END $$;
                    """
                            .formatted(RESTRICTED_ROLE, RESTRICTED_ROLE, RESTRICTED_ROLE));
            statement.execute(
                    "CREATE ROLE " + RESTRICTED_ROLE + " LOGIN PASSWORD '" + RESTRICTED_PASSWORD + "' NOSUPERUSER NOBYPASSRLS");
            statement.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON dummy_documents TO " + RESTRICTED_ROLE);
            statement.execute("GRANT USAGE ON SCHEMA public TO " + RESTRICTED_ROLE);
        }
    }

    private Connection adminConnection() throws SQLException {
        return DriverManager.getConnection(
                environment.getProperty("spring.datasource.url"),
                environment.getProperty("spring.datasource.username"),
                environment.getProperty("spring.datasource.password"));
    }

    private Connection restrictedConnection() throws SQLException {
        return DriverManager.getConnection(
                environment.getProperty("spring.datasource.url"), RESTRICTED_ROLE, RESTRICTED_PASSWORD);
    }
}
