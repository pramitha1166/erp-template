package com.eudext.erp.admin.internal.tenant;

import com.eudext.erp.admin.AdminAuditEvents;
import com.eudext.erp.config.tenancy.SuspendedTenantRegistry;
import com.eudext.erp.config.tenancy.TenantContextScope;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ADM-1 / ADM-5 / ADM-6: tenant reads and suspension/reactivation. */
@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final SuspendedTenantRegistry suspendedTenantRegistry;
    private final ApplicationEventPublisher events;

    public TenantService(
            TenantRepository tenantRepository, SuspendedTenantRegistry suspendedTenantRegistry, ApplicationEventPublisher events) {
        this.tenantRepository = tenantRepository;
        this.suspendedTenantRegistry = suspendedTenantRegistry;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Tenant get(UUID tenantId) {
        return tenantRepository.findById(tenantId).orElseThrow(() -> new NoSuchElementException("No such tenant"));
    }

    @Transactional(readOnly = true)
    public List<Tenant> listAll() {
        return tenantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Tenant> listByBrand(UUID brandId) {
        return tenantRepository.findByBrandId(brandId);
    }

    /** @throws IllegalArgumentException if {@code tenantId} does not belong to {@code brandId} — ADM-5's "own brand only" boundary. */
    public Tenant requireOwnedByBrand(UUID tenantId, UUID brandId) {
        Tenant tenant = get(tenantId);
        if (!tenant.getBrandId().equals(brandId)) {
            throw new IllegalArgumentException("Tenant does not belong to this brand");
        }
        return tenant;
    }

    @Transactional
    public void suspend(UUID tenantId, String reason, String actor) {
        Tenant tenant = get(tenantId);
        if (!tenant.isActive()) {
            return;
        }
        tenant.suspend(reason);
        tenantRepository.save(tenant);
        try (var scope = TenantContextScope.enter(tenantId)) {
            suspendedTenantRegistry.suspend(tenantId, reason);
        }
        events.publishEvent(new AdminAuditEvents.TenantSuspended(tenantId, reason, actor, Instant.now()));
    }

    @Transactional
    public void reactivate(UUID tenantId, String actor) {
        Tenant tenant = get(tenantId);
        if (tenant.isActive()) {
            return;
        }
        tenant.reactivate();
        tenantRepository.save(tenant);
        try (var scope = TenantContextScope.enter(tenantId)) {
            suspendedTenantRegistry.reactivate(tenantId);
        }
        events.publishEvent(new AdminAuditEvents.TenantReactivated(tenantId, actor, Instant.now()));
    }
}
