package com.eudext.erp;

import com.eudext.erp.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Smoke test proving the Testcontainers harness (0.0.9): the full Spring
 * context boots and Flyway migrates against a real, disposable Postgres.
 */
class ErpApplicationIT extends AbstractIntegrationTest {

    @Test
    void contextLoadsAgainstRealDatabase() {
        // Intentionally empty: a failure to start the context fails the test.
    }
}
