package com.eudext.erp.workflow.internal.web;

import com.eudext.erp.workflow.internal.engine.WorkflowEngine;
import com.eudext.erp.workflow.internal.instance.ApprovalHistoryEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** WF-7: a document's approval history — timestamps and comments, across every instance it has ever had. */
@RestController
@RequestMapping("/workflow/history")
public class ApprovalHistoryController {

    private static final String PERMISSION_VIEW = "workflow:approval-history:view";

    private final WorkflowEngine engine;
    private final WorkflowAccessControl accessControl;

    public ApprovalHistoryController(WorkflowEngine engine, WorkflowAccessControl accessControl) {
        this.engine = engine;
        this.accessControl = accessControl;
    }

    public record HistoryEntryView(UUID instanceId, String action, UUID actorUserId, String comment, Instant occurredAt) {
        static HistoryEntryView from(ApprovalHistoryEntry entry) {
            return new HistoryEntryView(entry.getInstanceId(), entry.getAction(), entry.getActorUserId(), entry.getComment(), entry.getOccurredAt());
        }
    }

    @GetMapping
    public List<HistoryEntryView> historyOf(
            @RequestParam UUID companyId, @RequestParam String documentType, @RequestParam UUID documentId) {
        accessControl.requirePermission(companyId, PERMISSION_VIEW);
        return engine.historyOf(documentType, documentId).stream().map(HistoryEntryView::from).toList();
    }
}
