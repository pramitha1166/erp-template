package com.eudext.erp.audit.internal.web;

import com.eudext.erp.audit.internal.history.DocumentHistoryService;
import com.eudext.erp.audit.internal.log.AuditAction;
import com.eudext.erp.audit.internal.log.AuditLogEntry;
import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.iam.PermissionApi;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AUD-4: read-only API behind a document/record's version-history view.
 * The UI itself is a later, frontend-phase concern (see {@code
 * TASK-BREAKDOWN.md} — no epic before this one has shipped a frontend page
 * either); this is the data it will render.
 */
@RestController
@RequestMapping("/audit/entities")
public class AuditHistoryController {

    private static final String PERMISSION_READ_AUDIT_LOG = "audit:entry:read";

    private final DocumentHistoryService historyService;
    private final PermissionApi permissionApi;

    public AuditHistoryController(DocumentHistoryService historyService, PermissionApi permissionApi) {
        this.historyService = historyService;
        this.permissionApi = permissionApi;
    }

    public record HistoryEntryView(
            UUID id, AuditAction action, String actor, Instant occurredAt, Map<String, Object> oldValues, Map<String, Object> newValues) {}

    @GetMapping("/{entityType}/{entityId}/history")
    public List<HistoryEntryView> history(
            @PathVariable String entityType, @PathVariable String entityId, @RequestParam UUID companyId) {
        requirePermission(companyId);
        return historyService.historyOf(tenantId(), entityType, entityId).stream()
                .map(entry -> new HistoryEntryView(
                        entry.getId(),
                        entry.getAction(),
                        entry.getActor(),
                        entry.getOccurredAt(),
                        entry.getOldValues(),
                        entry.getNewValues()))
                .toList();
    }

    private void requirePermission(UUID companyId) {
        if (!permissionApi.hasPermission(currentUserId(), companyId, PERMISSION_READ_AUDIT_LOG)) {
            throw new AccessDeniedException("Missing permission: " + PERMISSION_READ_AUDIT_LOG);
        }
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        return UUID.fromString(authentication.getName());
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
