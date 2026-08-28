package com.eudext.erp.admin.internal.web;

import com.eudext.erp.admin.internal.checklist.ChecklistItemKey;
import com.eudext.erp.admin.internal.checklist.ChecklistService;
import com.eudext.erp.config.tenancy.TenantContext;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADM-4: the tenant admin's own post-onboarding setup checklist — scoped
 * to the caller's own tenant (checked directly against {@code
 * TenantContext} rather than an admin permission, since this isn't a
 * platform/brand-admin console operation at all).
 */
@RestController
@RequestMapping("/admin/tenants/{tenantId}/checklist")
public class ChecklistController {

    private final ChecklistService checklistService;

    public ChecklistController(ChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    public record ChecklistItemView(String itemKey, boolean completed) {}

    public record SetCompletedRequest(boolean completed) {}

    @GetMapping
    public List<ChecklistItemView> list(@PathVariable UUID tenantId) {
        requireOwnTenant(tenantId);
        return checklistService.list(tenantId).stream()
                .map(item -> new ChecklistItemView(item.getItemKey().name(), item.isCompleted()))
                .toList();
    }

    @PutMapping("/{itemKey}")
    public void setCompleted(@PathVariable UUID tenantId, @PathVariable ChecklistItemKey itemKey, @RequestBody SetCompletedRequest request) {
        requireOwnTenant(tenantId);
        checklistService.setCompleted(tenantId, itemKey, request.completed());
    }

    private static void requireOwnTenant(UUID tenantId) {
        if (!tenantId.equals(TenantContext.get().orElse(null))) {
            throw new AccessDeniedException("Not a member of this tenant");
        }
    }
}
