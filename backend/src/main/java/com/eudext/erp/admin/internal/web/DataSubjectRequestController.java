package com.eudext.erp.admin.internal.web;

import com.eudext.erp.admin.internal.datarequest.DataRequestType;
import com.eudext.erp.admin.internal.datarequest.DataSubjectRequest;
import com.eudext.erp.admin.internal.datarequest.DataSubjectRequestService;
import com.eudext.erp.admin.internal.support.AdminAccessGuard;
import com.eudext.erp.admin.internal.support.AdminPermissions;
import com.eudext.erp.admin.internal.tenant.Tenant;
import com.eudext.erp.admin.internal.tenant.TenantService;
import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.iam.PermissionApi;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADM-8 / NFR-S7 / NFR-D5: tenant-initiated data export/erasure requests.
 * Reachable either by the tenant's own admin (self-service, checked
 * against {@code TenantContext} like {@code ChecklistController}) or by a
 * platform/brand admin acting on the tenant's behalf.
 */
@RestController
@RequestMapping("/admin/tenants/{tenantId}/data-requests")
public class DataSubjectRequestController {

    /** Proxy for "is an administrative user of this tenant" — the only permission granted to every tenant-admin role at onboarding (see {@code TenantOnboardingService}). */
    private static final String TENANT_ADMIN_PROXY_PERMISSION = "iam:role:manage";

    private final DataSubjectRequestService requestService;
    private final TenantService tenantService;
    private final AdminAccessGuard accessGuard;
    private final PermissionApi permissionApi;

    public DataSubjectRequestController(
            DataSubjectRequestService requestService,
            TenantService tenantService,
            AdminAccessGuard accessGuard,
            PermissionApi permissionApi) {
        this.requestService = requestService;
        this.tenantService = tenantService;
        this.accessGuard = accessGuard;
        this.permissionApi = permissionApi;
    }

    public record CreateRequest(@NotNull DataRequestType type, String notes) {}

    public record RequestView(UUID id, String type, String status, String resultPayload) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequestView submit(@PathVariable UUID tenantId, @RequestBody CreateRequest request) {
        String requestedBy = requireAccess(tenantId);
        return toView(requestService.submit(tenantId, request.type(), requestedBy, request.notes()));
    }

    @GetMapping
    public List<RequestView> list(@PathVariable UUID tenantId) {
        requireAccess(tenantId);
        return requestService.list(tenantId).stream().map(DataSubjectRequestController::toView).toList();
    }

    /**
     * ERASURE disables the tenant's admin account and suspends the whole
     * tenant (see {@code DataSubjectRequestService.performErasure}) — too
     * destructive to gate on "any authenticated user of this tenant".
     * Self-service still requires the caller to hold the same permission
     * only the tenant-admin role is granted at onboarding, not just tenant
     * membership.
     */
    private String requireAccess(UUID tenantId) {
        UUID ambientTenantId = TenantContext.get().orElse(null);
        if (tenantId.equals(ambientTenantId)) {
            Tenant tenant = tenantService.get(tenantId);
            UUID userId = accessGuard.currentUserId();
            if (tenant.getPrimaryCompanyId() == null
                    || !permissionApi.hasPermission(userId, tenant.getPrimaryCompanyId(), TENANT_ADMIN_PROXY_PERMISSION)) {
                throw new AccessDeniedException("Not authorized for this tenant's data requests");
            }
            return userId.toString();
        }
        UUID brandId = tenantService.get(tenantId).getBrandId();
        return accessGuard.requireBrandAccess(brandId, AdminPermissions.DATA_REQUEST_MANAGE).toString();
    }

    private static RequestView toView(DataSubjectRequest request) {
        return new RequestView(request.getId(), request.getType().name(), request.getStatus().name(), request.getResultPayloadJson());
    }
}
