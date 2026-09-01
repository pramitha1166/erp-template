package com.eudext.erp.workflow;

/** The acting user is neither the task's assigned approver nor an active delegate of theirs (WF-5). */
public class NotAssignedApproverException extends RuntimeException {

    public NotAssignedApproverException(String message) {
        super(message);
    }
}
