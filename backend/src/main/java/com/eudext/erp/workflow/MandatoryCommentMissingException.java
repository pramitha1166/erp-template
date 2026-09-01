package com.eudext.erp.workflow;

/** WF-6: a rejection was attempted without the mandatory comment. */
public class MandatoryCommentMissingException extends RuntimeException {

    public MandatoryCommentMissingException(String message) {
        super(message);
    }
}
