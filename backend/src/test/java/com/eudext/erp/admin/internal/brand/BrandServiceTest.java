package com.eudext.erp.admin.internal.brand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eudext.erp.iam.IdentityProvisioningApi;
import com.eudext.erp.notification.NotificationApi;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** ADM-1: Brand lifecycle. */
@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository repository;

    @Mock
    private IdentityProvisioningApi identityProvisioningApi;

    @Mock
    private NotificationApi notificationApi;

    @Mock
    private ApplicationEventPublisher events;

    private BrandService service;
    private final UUID brandId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BrandService(repository, identityProvisioningApi, notificationApi, events);
    }

    @Test
    void suspendingAnAlreadySuspendedBrandIsANoOp() {
        Brand brand = Brand.create("Acme", "Acme Pvt Ltd", "support@acme.test");
        brand.suspend("initial suspension");
        when(repository.findById(brandId)).thenReturn(Optional.of(brand));

        service.suspend(brandId, "second suspension", "actor");

        verify(repository, never()).save(ArgumentMatchers.any());
        verify(events, never()).publishEvent(ArgumentMatchers.any());
    }

    @Test
    void suspendingAnActiveBrandPersistsAndPublishes() {
        Brand brand = Brand.create("Acme", "Acme Pvt Ltd", "support@acme.test");
        when(repository.findById(brandId)).thenReturn(Optional.of(brand));

        service.suspend(brandId, "non-payment", "actor");

        assertThat(brand.isActive()).isFalse();
        verify(repository).save(brand);
        verify(events).publishEvent(ArgumentMatchers.any(Object.class));
    }

    @Test
    void reactivatingAnActiveBrandIsANoOp() {
        Brand brand = Brand.create("Acme", "Acme Pvt Ltd", "support@acme.test");
        when(repository.findById(brandId)).thenReturn(Optional.of(brand));

        service.reactivate(brandId, "actor");

        verify(repository, never()).save(ArgumentMatchers.any());
    }
}
