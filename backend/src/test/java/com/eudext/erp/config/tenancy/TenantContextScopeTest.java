package com.eudext.erp.config.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** ADM-1/ADM-5/ADM-9: proves the scope restores whatever tenant was ambient before it, including "none". */
class TenantContextScopeTest {

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void restoresPreviousTenantOnClose() {
        UUID original = UUID.randomUUID();
        UUID scoped = UUID.randomUUID();
        TenantContext.set(original);

        try (var scope = TenantContextScope.enter(scoped)) {
            assertThat(TenantContext.get()).contains(scoped);
        }

        assertThat(TenantContext.get()).contains(original);
    }

    @Test
    void restoresEmptyWhenNothingWasAmbientBefore() {
        TenantContext.clear();
        UUID scoped = UUID.randomUUID();

        try (var scope = TenantContextScope.enter(scoped)) {
            assertThat(TenantContext.get()).contains(scoped);
        }

        assertThat(TenantContext.get()).isEmpty();
    }

    @Test
    void nestedScopesRestoreCorrectlyInReverseOrder() {
        UUID outer = UUID.randomUUID();
        UUID inner = UUID.randomUUID();

        try (var outerScope = TenantContextScope.enter(outer)) {
            try (var innerScope = TenantContextScope.enter(inner)) {
                assertThat(TenantContext.get()).contains(inner);
            }
            assertThat(TenantContext.get()).contains(outer);
        }
        assertThat(TenantContext.get()).isEmpty();
    }
}
