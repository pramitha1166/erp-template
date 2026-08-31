package com.eudext.erp.config.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuspendedTenantRegistryTest {

    @Mock
    private SuspendedTenantRepository repository;

    private SuspendedTenantRegistry registry;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        registry = new SuspendedTenantRegistry(repository);
    }

    @Test
    void requireActiveIsNoOpWhenNotSuspended() {
        when(repository.existsById(tenantId)).thenReturn(false);

        registry.requireActive(tenantId);
    }

    @Test
    void requireActiveThrowsWhenSuspended() {
        when(repository.existsById(tenantId)).thenReturn(true);

        assertThatThrownBy(() -> registry.requireActive(tenantId))
                .isInstanceOf(TenantSuspendedException.class)
                .satisfies(e -> assertThat(((TenantSuspendedException) e).getTenantId()).isEqualTo(tenantId));
    }

    @Test
    void suspendIsIdempotent() {
        when(repository.existsById(tenantId)).thenReturn(false);

        registry.suspend(tenantId, "non-payment");

        ArgumentCaptor<SuspendedTenantMarker> captor = ArgumentCaptor.forClass(SuspendedTenantMarker.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(captor.getValue().getReason()).isEqualTo("non-payment");
    }

    @Test
    void suspendDoesNothingIfAlreadySuspended() {
        when(repository.existsById(tenantId)).thenReturn(true);

        registry.suspend(tenantId, "non-payment");

        verify(repository, org.mockito.Mockito.never()).save(org.mockito.Mockito.any());
    }

    @Test
    void reactivateDeletesTheMarker() {
        registry.reactivate(tenantId);

        verify(repository).deleteById(tenantId);
    }
}
