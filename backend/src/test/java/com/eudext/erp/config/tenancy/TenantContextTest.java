package com.eudext.erp.config.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void isEmptyByDefault() {
        assertThat(TenantContext.get()).isEmpty();
    }

    @Test
    void returnsWhatWasSet() {
        UUID tenantId = UUID.randomUUID();

        TenantContext.set(tenantId);

        assertThat(TenantContext.get()).contains(tenantId);
    }

    @Test
    void clearRemovesTheValue() {
        TenantContext.set(UUID.randomUUID());

        TenantContext.clear();

        assertThat(TenantContext.get()).isEmpty();
    }

    @Test
    void settingNullClearsTheValue() {
        TenantContext.set(UUID.randomUUID());

        TenantContext.set(null);

        assertThat(TenantContext.get()).isEmpty();
    }

    @Test
    void isIsolatedPerThread() throws InterruptedException {
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);

        UUID[] seenOnOtherThread = new UUID[1];
        Thread other = new Thread(() -> seenOnOtherThread[0] = TenantContext.get().orElse(null));
        other.start();
        other.join();

        assertThat(seenOnOtherThread[0]).isNull();
        assertThat(TenantContext.get()).contains(tenantId);
    }
}
