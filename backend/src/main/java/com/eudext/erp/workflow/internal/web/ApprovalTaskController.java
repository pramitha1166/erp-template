package com.eudext.erp.workflow.internal.web;

import com.eudext.erp.workflow.internal.engine.WorkflowEngine;
import com.eudext.erp.workflow.internal.instance.TaskStatus;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * WF-6 / WF-8: acting on assigned approval tasks, and the "pending
 * approval" in-app inbox ({@code /workflow/tasks/mine}) F0.4.3's frontend
 * widget is expected to read.
 */
@RestController
@RequestMapping("/workflow/tasks")
public class ApprovalTaskController {

    private static final String PERMISSION_APPROVE = "workflow:approval-task:approve";

    private final WorkflowEngine engine;
    private final WorkflowAccessControl accessControl;

    public ApprovalTaskController(WorkflowEngine engine, WorkflowAccessControl accessControl) {
        this.engine = engine;
        this.accessControl = accessControl;
    }

    public record TaskView(
            UUID id, UUID instanceId, String documentType, UUID documentId, TaskStatus status, Instant dueAt) {}

    @GetMapping("/mine")
    public List<TaskView> myPendingTasks() {
        UUID userId = accessControl.currentUserId();
        return engine.pendingTasksFor(userId).stream().map(this::toView).toList();
    }

    public record DecisionRequest(String comment) {}

    @PostMapping("/{taskId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable UUID taskId, @RequestParam UUID companyId, @Valid @RequestBody(required = false) DecisionRequest request) {
        UUID userId = accessControl.requirePermission(companyId, PERMISSION_APPROVE);
        engine.decide(taskId, userId, true, request == null ? null : request.comment());
    }

    /** WF-6: rejection always requires a comment — enforced by {@code WorkflowEngine}, not merely by request validation. */
    @PostMapping("/{taskId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable UUID taskId, @RequestParam UUID companyId, @Valid @RequestBody DecisionRequest request) {
        UUID userId = accessControl.requirePermission(companyId, PERMISSION_APPROVE);
        engine.decide(taskId, userId, false, request.comment());
    }

    private TaskView toView(WorkflowEngine.TaskWithDocument taskWithDocument) {
        var task = taskWithDocument.task();
        var instance = taskWithDocument.instance();
        return new TaskView(task.getId(), instance.getId(), instance.getDocumentType(), instance.getDocumentId(), task.getStatus(), task.getDueAt());
    }
}
