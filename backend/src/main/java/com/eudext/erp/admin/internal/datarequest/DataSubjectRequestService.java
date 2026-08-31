package com.eudext.erp.admin.internal.datarequest;

import com.eudext.erp.admin.AdminAuditEvents;
import com.eudext.erp.admin.internal.tenant.Tenant;
import com.eudext.erp.admin.internal.tenant.TenantService;
import com.eudext.erp.config.tenancy.TenantContextScope;
import com.eudext.erp.iam.IdentityProvisioningApi;
import com.eudext.erp.masterdata.MasterDataProvisioningApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ADM-8 / NFR-S7 / NFR-D5: PDPA data export and erasure request workflow. */
@Service
public class DataSubjectRequestService {

    private static final Logger log = LoggerFactory.getLogger(DataSubjectRequestService.class);

    private final DataSubjectRequestRepository repository;
    private final TenantService tenantService;
    private final MasterDataProvisioningApi masterDataApi;
    private final IdentityProvisioningApi identityProvisioningApi;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher events;

    public DataSubjectRequestService(
            DataSubjectRequestRepository repository,
            TenantService tenantService,
            MasterDataProvisioningApi masterDataApi,
            IdentityProvisioningApi identityProvisioningApi,
            ObjectMapper objectMapper,
            ApplicationEventPublisher events) {
        this.repository = repository;
        this.tenantService = tenantService;
        this.masterDataApi = masterDataApi;
        this.identityProvisioningApi = identityProvisioningApi;
        this.objectMapper = objectMapper;
        this.events = events;
    }

    @Transactional
    public DataSubjectRequest submit(UUID tenantId, DataRequestType type, String requestedBy, String notes) {
        DataSubjectRequest request;
        try (var scope = TenantContextScope.enter(tenantId)) {
            request = repository.save(DataSubjectRequest.create(tenantId, type, requestedBy, notes));
        }
        events.publishEvent(
                new AdminAuditEvents.DataSubjectRequestCreated(request.getId(), tenantId, type.name(), requestedBy, Instant.now()));
        process(tenantId, request.getId());
        return get(tenantId, request.getId());
    }

    @Transactional(readOnly = true)
    public List<DataSubjectRequest> list(UUID tenantId) {
        try (var scope = TenantContextScope.enter(tenantId)) {
            return repository.findByTenantId(tenantId);
        }
    }

    @Transactional(readOnly = true)
    public DataSubjectRequest get(UUID tenantId, UUID requestId) {
        try (var scope = TenantContextScope.enter(tenantId)) {
            return repository.findById(requestId).orElseThrow();
        }
    }

    @Transactional
    void process(UUID tenantId, UUID requestId) {
        try (var scope = TenantContextScope.enter(tenantId)) {
            DataSubjectRequest request = repository.findById(requestId).orElseThrow();
            try {
                String resultJson = request.getType() == DataRequestType.EXPORT
                        ? objectMapper.writeValueAsString(buildExportBundle(tenantId))
                        : objectMapper.writeValueAsString(performErasure(tenantId));
                request.complete(resultJson);
                repository.save(request);
                events.publishEvent(new AdminAuditEvents.DataSubjectRequestCompleted(
                        requestId, tenantId, request.getType().name(), Instant.now()));
            } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
                log.error("Data subject request {} failed", requestId, e);
                request.fail(e.getMessage());
                repository.save(request);
            }
        }
    }

    /** NFR-D5: everything the platform currently holds about the tenant — see the entity javadoc for why this is inline JSON, not S3. */
    private Map<String, Object> buildExportBundle(UUID tenantId) {
        Tenant tenant = tenantService.get(tenantId);
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("tenantId", tenantId);
        bundle.put("tenantName", tenant.getName());
        bundle.put("tenantStatus", tenant.getStatus().name());
        bundle.put("activeUserCount", identityProvisioningApi.countActiveUsers());
        if (tenant.getPrimaryCompanyId() != null) {
            MasterDataProvisioningApi.CompanyView company = masterDataApi.getCompany(tenant.getPrimaryCompanyId());
            bundle.put("company", Map.of(
                    "id", company.id(), "legalName", company.legalName(), "baseCurrency", company.baseCurrency()));
        }
        bundle.put("exportedAt", Instant.now().toString());
        return bundle;
    }

    /**
     * ADM-8: erasure disables the tenant's accounts and marks its company
     * record inactive — it does not hard-delete rows. AUD-5 requires a
     * 7-year minimum audit retention, and FIN-4/ARCH ledger immutability
     * means transactional history (once Phase 1 modules create any) can
     * never be purged either; full anonymization of a legally-retained
     * audit trail is out of this epic's scope and would need its own
     * policy decision, not a silent side effect of an erasure request.
     */
    private Map<String, Object> performErasure(UUID tenantId) {
        Tenant tenant = tenantService.get(tenantId);
        if (tenant.getPrimaryAdminUserId() != null) {
            identityProvisioningApi.setUserActive(tenantId, tenant.getPrimaryAdminUserId(), false);
        }
        if (tenant.getPrimaryCompanyId() != null) {
            masterDataApi.disableCompany(tenant.getPrimaryCompanyId());
        }
        tenantService.suspend(tenantId, "Erasure request completed", "system:erasure");
        return Map.of(
                "note",
                "Accounts disabled and company marked inactive; audit trail retained per AUD-5's 7-year minimum retention.",
                "erasedAt",
                Instant.now().toString());
    }
}
