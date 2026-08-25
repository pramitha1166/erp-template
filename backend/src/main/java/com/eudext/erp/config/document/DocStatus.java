package com.eudext.erp.config.document;

/**
 * ARCH-4 document lifecycle states: {@code DRAFT -> SUBMITTED -> CANCELLED},
 * with {@code AMENDED} marking a submitted document as superseded by a new
 * linked document (the new document starts its own lifecycle at
 * {@code DRAFT}). {@code CANCELLED} and {@code AMENDED} are terminal.
 */
public enum DocStatus {
    DRAFT,
    SUBMITTED,
    CANCELLED,
    AMENDED
}
