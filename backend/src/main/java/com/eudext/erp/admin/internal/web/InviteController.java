package com.eudext.erp.admin.internal.web;

import com.eudext.erp.admin.internal.invite.TenantAdminInviteService;
import com.eudext.erp.admin.internal.support.AdminAccessGuard;
import com.eudext.erp.admin.internal.support.AdminPermissions;
import com.eudext.erp.admin.internal.tenant.TenantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** ADM-5 / BRD-14: brand-admin invite flow for additional/replacement tenant-admin users. */
@RestController
@RequestMapping("/admin/tenants/{tenantId}/invites")
public class InviteController {

    private final TenantAdminInviteService inviteService;
    private final TenantService tenantService;
    private final AdminAccessGuard accessGuard;

    public InviteController(TenantAdminInviteService inviteService, TenantService tenantService, AdminAccessGuard accessGuard) {
        this.inviteService = inviteService;
        this.tenantService = tenantService;
        this.accessGuard = accessGuard;
    }

    public record CreateInviteRequest(@Email @NotBlank String email) {}

    public record InviteView(UUID id, String email, String status) {}

    public record AcceptInviteRequest(@NotBlank String token, @NotBlank @Size(min = 12) String password) {}

    public record AcceptedView(UUID userId) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void invite(@PathVariable UUID tenantId, @Valid @RequestBody CreateInviteRequest request) {
        UUID brandId = tenantService.get(tenantId).getBrandId();
        UUID actor = accessGuard.requireBrandAccess(brandId, AdminPermissions.TENANT_INVITE);
        inviteService.invite(tenantId, request.email(), actor.toString());
    }

    @GetMapping
    public List<InviteView> list(@PathVariable UUID tenantId) {
        UUID brandId = tenantService.get(tenantId).getBrandId();
        accessGuard.requireBrandAccess(brandId, AdminPermissions.TENANT_INVITE);
        return inviteService.list(tenantId).stream()
                .map(invite -> new InviteView(invite.getId(), invite.getEmail(), invite.getStatus().name()))
                .toList();
    }

    @DeleteMapping("/{inviteId}")
    public void revoke(@PathVariable UUID tenantId, @PathVariable UUID inviteId) {
        UUID brandId = tenantService.get(tenantId).getBrandId();
        accessGuard.requireBrandAccess(brandId, AdminPermissions.TENANT_INVITE);
        inviteService.revoke(tenantId, inviteId);
    }

    /** ADM-5: reached before the invitee has any session — see {@code SecurityConfig}'s public-paths entry. */
    @PostMapping("/accept")
    @ResponseStatus(HttpStatus.CREATED)
    public AcceptedView accept(@PathVariable UUID tenantId, @Valid @RequestBody AcceptInviteRequest request) {
        UUID userId = inviteService.accept(tenantId, request.token(), request.password());
        return new AcceptedView(userId);
    }
}
