package com.eudext.erp.workflow;

/** An operation was attempted against a workflow instance or task in a state that does not permit it. */
public class IllegalWorkflowStateException extends RuntimeException {

    public IllegalWorkflowStateException(String message) {
        super(message);
    }
}
