package com.eudext.erp.admin.internal.web;

import com.eudext.erp.admin.internal.impersonation.ImpersonationService;
import com.eudext.erp.admin.internal.support.AdminAccessGuard;
import com.eudext.erp.admin.internal.support.AdminPermissions;
import com.eudext.erp.admin.internal.tenant.TenantService;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** ADM-7: support impersonation — time-boxed platform/brand-admin-as-tenant-admin sessions. */
@RestController
@RequestMapping("/admin/tenants/{tenantId}/impersonation")
public class ImpersonationController {

    private final ImpersonationService impersonationService;
    private final TenantService tenantService;
    private final AdminAccessGuard accessGuard;

    public ImpersonationController(ImpersonationService impersonationService, TenantService tenantService, AdminAccessGuard accessGuard) {
        this.impersonationService = impersonationService;
        this.tenantService = tenantService;
        this.accessGuard = accessGuard;
    }

    public record StartRequest(@NotBlank String reason) {}

    public record StartedView(UUID sessionId, String token, Instant expiresAt) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StartedView start(@PathVariable UUID tenantId, @RequestBody StartRequest request) {
        UUID brandId = tenantService.get(tenantId).getBrandId();
        UUID actor = accessGuard.requireBrandAccess(brandId, AdminPermissions.IMPERSONATION_START);
        var session = impersonationService.start(tenantId, actor, request.reason());
        return new StartedView(session.sessionId(), session.token(), session.expiresAt());
    }

    @PostMapping("/{sessionId}/end")
    public void end(@PathVariable UUID tenantId, @PathVariable UUID sessionId) {
        UUID brandId = tenantService.get(tenantId).getBrandId();
        accessGuard.requireBrandAccess(brandId, AdminPermissions.IMPERSONATION_START);
        impersonationService.end(tenantId, sessionId);
    }
}
