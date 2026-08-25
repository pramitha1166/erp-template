package com.eudext.erp.audit.internal.log;

/** AUD-1: the three mutation kinds an audit_log row can record. */
public enum AuditAction {
    INSERT,
    UPDATE,
    DELETE
}
