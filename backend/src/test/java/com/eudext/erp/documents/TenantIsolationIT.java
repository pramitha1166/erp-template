package com.eudext.erp.documents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.documents.internal.dummy.DummyDocument;
import com.eudext.erp.documents.internal.dummy.DummyDocumentRepository;
import com.eudext.erp.documents.internal.dummy.DummyDocumentService;
import com.eudext.erp.testsupport.AbstractIntegrationTest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * NFR-S6 / Phase 0 gate criterion: "a request in tenant A must never see
 * tenant B data." Proves the {@code tenant_isolation} RLS policy from
 * {@code V3__dummy_document.sql} actually blocks cross-tenant access — not
 * just that the application-layer code happens to filter correctly.
 *
 * <p>Deliberately runs every assertion through a hand-rolled JDBC
 * connection authenticated as a dedicated, non-superuser role created just
 * for this test, rather than through the app's own connection pool: the
 * Testcontainers Postgres bootstrap role (like a plain {@code postgres}
 * superuser anywhere) bypasses row-level security entirely, so exercising
 * RLS through it would make this test pass even if the policy were broken
 * or missing. A restricted role is representative of how the database
 * behaves for the eventual application role in every real deployment
 * (an RDS master user is not a true Postgres superuser either).
 */
class TenantIsolationIT extends AbstractIntegrationTest {

    private static final String RESTRICTED_ROLE = "tenant_isolation_test_role";
    private static final String RESTRICTED_PASSWORD = "tenant-isolation-test";

    @Autowired
    private DummyDocumentService service;

    @Autowired
    private DummyDocumentRepository repository;

    @Autowired
    private Environment environment;

    private final UUID companyId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();

    private UUID tenantA;
    private UUID tenantB;
    private UUID tenantADocumentId;
    private UUID tenantBDocumentId;

    @BeforeEach
    void seedTwoTenants() throws SQLException {
        provisionRestrictedRole();

        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();

        TenantContext.set(tenantA);
        tenantADocumentId = service.create(tenantA, companyId, branchId, "tenant A secret").getId();

        TenantContext.set(tenantB);
        tenantBDocumentId = service.create(tenantB, companyId, branchId, "tenant B secret").getId();

        TenantContext.clear();
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void tenantOnlySeesItsOwnDocuments() throws SQLException {
        try (Connection connection = restrictedConnection()) {
            setSessionTenant(connection, tenantA);
            assertThat(visibleDocumentIds(connection)).containsExactly(tenantADocumentId);

            setSessionTenant(connection, tenantB);
            assertThat(visibleDocumentIds(connection)).containsExactly(tenantBDocumentId);
        }
    }

    @Test
    void noTenantContextSeesNothing() throws SQLException {
        try (Connection connection = restrictedConnection()) {
            setSessionTenant(connection, null);
            assertThat(visibleDocumentIds(connection)).isEmpty();
        }
    }

    @Test
    void tenantCannotUpdateAnotherTenantsDocument() throws SQLException {
        try (Connection connection = restrictedConnection()) {
            setSessionTenant(connection, tenantA);
            try (PreparedStatement update =
                    connection.prepareStatement("UPDATE dummy_documents SET note = ? WHERE id = ?")) {
                update.setString(1, "hacked");
                update.setObject(2, tenantBDocumentId);
                assertThat(update.executeUpdate()).isZero();
            }
        }

        DummyDocument stillIntact = repository.findById(tenantBDocumentId).orElseThrow();
        assertThat(stillIntact.getNote()).isEqualTo("tenant B secret");
    }

    @Test
    void tenantCannotDeleteAnotherTenantsDocument() throws SQLException {
        try (Connection connection = restrictedConnection()) {
            setSessionTenant(connection, tenantA);
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM dummy_documents WHERE id = ?")) {
                delete.setObject(1, tenantBDocumentId);
                assertThat(delete.executeUpdate()).isZero();
            }
        }
    }

    @Test
    void tenantCannotInsertARowClaimingAnotherTenant() throws SQLException {
        try (Connection connection = restrictedConnection()) {
            setSessionTenant(connection, tenantA);
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO dummy_documents (id, tenant_id, company_id, branch_id, doc_status, posting_date) "
                            + "VALUES (?, ?, ?, ?, 'DRAFT', CURRENT_DATE)")) {
                insert.setObject(1, UUID.randomUUID());
                insert.setObject(2, tenantB);
                insert.setObject(3, companyId);
                insert.setObject(4, branchId);
                assertThatThrownBy(insert::executeUpdate).isInstanceOf(SQLException.class);
            }
        }
    }

    private void provisionRestrictedRole() throws SQLException {
        try (Connection admin = adminConnection();
                Statement statement = admin.createStatement()) {
            statement.execute(
                    """
                    DO $$
                    BEGIN
                        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'tenant_isolation_test_role') THEN
                            EXECUTE 'DROP OWNED BY tenant_isolation_test_role';
                            EXECUTE 'DROP ROLE tenant_isolation_test_role';
                        END IF;
                    END $$;
                    """);
            statement.execute("CREATE ROLE " + RESTRICTED_ROLE + " LOGIN PASSWORD '" + RESTRICTED_PASSWORD
                    + "' NOSUPERUSER NOBYPASSRLS");
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

    private void setSessionTenant(Connection connection, UUID tenantId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT set_config('app.tenant_id', ?, false)")) {
            statement.setString(1, tenantId == null ? "" : tenantId.toString());
            statement.execute();
        }
    }

    private List<UUID> visibleDocumentIds(Connection connection) throws SQLException {
        List<UUID> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM dummy_documents");
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add((UUID) resultSet.getObject("id"));
            }
        }
        return ids;
    }
}
