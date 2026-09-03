package com.eudext.erp.documents.internal.attachment;

/** DOC-4: outcome of the virus scan run on an attachment at upload time. */
public enum ScanStatus {
    /** No scanner configured in this environment (see {@code eudext.documents.attachments.virus-scan.enabled}). */
    PENDING,
    CLEAN,
    INFECTED,
    FAILED
}
