package com.eudext.erp.audit.internal.web;

import com.eudext.erp.audit.internal.history.DocumentHistoryService;
import com.eudext.erp.audit.internal.log.AuditAction;
import com.eudext.erp.audit.internal.log.AuditLogEntry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AUD-4: read-only API behind a document/record's version-history view.
 * Rendered by F0.3.1's version history panel.
 */
@RestController
@RequestMapping("/audit/entities")
public class AuditHistoryController {

    private final DocumentHistoryService historyService;
    private final AuditAccessGuard accessGuard;

    public AuditHistoryController(DocumentHistoryService historyService, AuditAccessGuard accessGuard) {
        this.historyService = historyService;
        this.accessGuard = accessGuard;
    }

    public record HistoryEntryView(
            UUID id, AuditAction action, String actor, Instant occurredAt, Map<String, Object> oldValues, Map<String, Object> newValues) {}

    @GetMapping("/{entityType}/{entityId}/history")
    public List<HistoryEntryView> history(
            @PathVariable String entityType, @PathVariable String entityId, @RequestParam UUID companyId) {
        accessGuard.requirePermission(companyId);
        return historyService.historyOf(accessGuard.tenantId(), entityType, entityId).stream()
                .map(AuditHistoryController::toView)
                .toList();
    }

    private static HistoryEntryView toView(AuditLogEntry entry) {
        return new HistoryEntryView(
                entry.getId(), entry.getAction(), entry.getActor(), entry.getOccurredAt(), entry.getOldValues(), entry.getNewValues());
    }
}
