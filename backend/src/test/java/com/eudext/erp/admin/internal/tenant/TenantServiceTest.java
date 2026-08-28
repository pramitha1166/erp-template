package com.eudext.erp.admin.internal.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eudext.erp.config.tenancy.SuspendedTenantRegistry;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** ADM-5 / ADM-6: tenant reads, brand-ownership boundary, and suspension/reactivation. */
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository repository;

    @Mock
    private SuspendedTenantRegistry suspendedTenantRegistry;

    @Mock
    private ApplicationEventPublisher events;

    private TenantService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID brandId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TenantService(repository, suspendedTenantRegistry, events);
    }

    @Test
    void requireOwnedByBrandPassesForTheOwningBrand() {
        Tenant tenant = Tenant.create(brandId, "Acme LK");
        when(repository.findById(tenantId)).thenReturn(Optional.of(tenant));

        assertThat(service.requireOwnedByBrand(tenantId, brandId)).isSameAs(tenant);
    }

    @Test
    void requireOwnedByBrandRejectsADifferentBrand() {
        Tenant tenant = Tenant.create(brandId, "Acme LK");
        when(repository.findById(tenantId)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.requireOwnedByBrand(tenantId, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void suspendMarksTheTenantAndTheRegistryAndPublishes() {
        Tenant tenant = Tenant.create(brandId, "Acme LK");
        when(repository.findById(tenantId)).thenReturn(Optional.of(tenant));

        service.suspend(tenantId, "non-payment", "actor");

        assertThat(tenant.isActive()).isFalse();
        verify(repository).save(tenant);
        verify(suspendedTenantRegistry).suspend(tenantId, "non-payment");
        verify(events).publishEvent(ArgumentMatchers.any(Object.class));
    }

    @Test
    void suspendingAnAlreadySuspendedTenantIsANoOp() {
        Tenant tenant = Tenant.create(brandId, "Acme LK");
        tenant.suspend("first");
        when(repository.findById(tenantId)).thenReturn(Optional.of(tenant));

        service.suspend(tenantId, "second", "actor");

        verify(suspendedTenantRegistry, never()).suspend(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void reactivateClearsTheRegistryMarker() {
        Tenant tenant = Tenant.create(brandId, "Acme LK");
        tenant.suspend("non-payment");
        when(repository.findById(tenantId)).thenReturn(Optional.of(tenant));

        service.reactivate(tenantId, "actor");

        assertThat(tenant.isActive()).isTrue();
        verify(suspendedTenantRegistry).reactivate(tenantId);
    }
}
