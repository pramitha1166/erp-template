package com.eudext.erp.workflow;

import java.time.Instant;
import java.util.UUID;

/**
 * WF-7 / WF-8: workflow domain events, published via Spring's application
 * event infrastructure so other modules (notification's WF-8 email leg,
 * a future audit listener) can subscribe with
 * {@code @ApplicationModuleListener} without workflow depending on them
 * (ARCH-1) — the same pattern {@code iam.AuthAuditEvents} established for
 * IAM-10.
 */
public final class WorkflowDecisionEvents {

    private WorkflowDecisionEvents() {}

    /** One approval task was created and assigned — WF-8's "pending approval" notification trigger. */
    public record ApprovalRequested(
            UUID tenantId,
            UUID companyId,
            String documentType,
            UUID documentId,
            UUID instanceId,
            UUID taskId,
            UUID assignedUserId,
            Instant occurredAt) {}

    /** Every step of the chain was satisfied — the document may now be submitted. */
    public record ApprovalGranted(
            UUID tenantId, UUID companyId, String documentType, UUID documentId, UUID instanceId, Instant occurredAt) {}

    /** WF-6: a step was rejected with a mandatory comment; the document stays in draft. */
    public record ApprovalRejected(
            UUID tenantId,
            UUID companyId,
            String documentType,
            UUID documentId,
            UUID instanceId,
            UUID rejectedBy,
            String comment,
            Instant occurredAt) {}

    /** WF-5: a task timed out and was reassigned. */
    public record TaskEscalated(
            UUID tenantId,
            UUID companyId,
            String documentType,
            UUID documentId,
            UUID instanceId,
            UUID fromUserId,
            UUID toUserId,
            UUID newTaskId,
            Instant occurredAt) {}
}
