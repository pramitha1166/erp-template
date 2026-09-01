package com.eudext.erp.workflow;

/** WF-3: a step's configured approver (role/user/hierarchy level) resolved to zero eligible users. */
public class NoApproverResolvedException extends RuntimeException {

    public NoApproverResolvedException(String message) {
        super(message);
    }
}
