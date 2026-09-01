package com.eudext.erp.workflow;

import java.util.Map;
import java.util.UUID;

/**
 * WF-1..WF-4: the integration point a document-owning module uses to gate
 * submission behind an approval chain, without reaching into workflow's
 * internal tables (ARCH-1).
 *
 * <p><b>Integration contract.</b> Call {@link #startApproval} while the
 * document is still {@code DRAFT}, <i>before</i> calling
 * {@code Document.submit()}. If it returns {@link Outcome#NOT_REQUIRED},
 * submit immediately — no chain applies. If it returns
 * {@link Outcome#PENDING}, do not submit yet; wait for the instance to
 * resolve (poll {@link #statusOf}, or react to
 * {@link WorkflowDecisionEvents.ApprovalGranted} /
 * {@link WorkflowDecisionEvents.ApprovalRejected}), then submit only once
 * it reaches {@code APPROVED}. On {@code REJECTED} the document simply
 * never leaves {@code DRAFT} — this is how WF-6 ("rejection returns the
 * document to draft") is satisfied: {@code Document} has no
 * {@code SUBMITTED -> DRAFT} transition to reuse, so workflow approval is
 * designed to run strictly pre-submit rather than to un-submit anything.
 */
public interface WorkflowApi {

    Outcome startApproval(StartApprovalRequest request);

    /** {@code null} if no approval instance has ever been started for this document. */
    Status statusOf(String documentType, UUID documentId);

    /** Cancels a still-pending instance (e.g. the document itself was cancelled or amended before approval finished). */
    void cancelPending(String documentType, UUID documentId);

    enum Outcome {
        /** No active chain applies to this (companyId, documentType) — or none of its steps' WF-2 conditions matched. */
        NOT_REQUIRED,
        /** A new approval instance was started; tasks were created for its first step group. */
        PENDING,
        /** A PENDING instance already exists for this document — the caller should wait on that one rather than starting another. */
        ALREADY_PENDING
    }

    enum Status {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED
    }

    /**
     * @param fieldValues the submitted document's field values, evaluated against WF-2 step conditions. Numeric
     *     values must be {@link java.math.BigDecimal} (ARCH-5) — never {@code double}/{@code float}.
     */
    record StartApprovalRequest(
            UUID tenantId,
            UUID companyId,
            UUID branchId,
            String documentType,
            UUID documentId,
            UUID submittedBy,
            Map<String, Object> fieldValues) {}
}
