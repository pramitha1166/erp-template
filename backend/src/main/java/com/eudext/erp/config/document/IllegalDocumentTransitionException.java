package com.eudext.erp.config.document;

/**
 * Thrown when code attempts a {@link DocStatus} transition the ARCH-4 state
 * machine does not allow (e.g. {@code CANCELLED -> SUBMITTED}).
 */
public class IllegalDocumentTransitionException extends RuntimeException {

    public IllegalDocumentTransitionException(DocStatus from, DocStatus to) {
        super("Cannot transition document from " + from + " to " + to);
    }
}
