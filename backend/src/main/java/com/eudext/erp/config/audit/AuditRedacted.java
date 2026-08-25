package com.eudext.erp.config.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AUD-2 requires capturing the old/new value of every changed field, but
 * some fields hold secret material (password hashes, TOTP seeds, token
 * hashes) that must never be readable from the audit trail even though the
 * fact that the field changed is still worth recording. A field carrying
 * this annotation still appears in a change entry the {@code audit}
 * module's interceptor records — its value is replaced with a fixed
 * redaction marker rather than the real old/new values. See {@link
 * NotAudited} for why this lives in {@code config} rather than {@code audit}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AuditRedacted {}
