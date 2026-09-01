package com.eudext.erp.workflow.internal.chain;

/** WF-3: how an {@link ApprovalStep} (or its escalation target) resolves to concrete approver user ids. */
public enum ApproverType {
    /** Every user holding the step's configured role in the instance's company. */
    ROLE,
    /** The step's single configured user id. */
    USER,
    /** Walks the submitter's manager chain up the configured number of levels. */
    HIERARCHY
}
