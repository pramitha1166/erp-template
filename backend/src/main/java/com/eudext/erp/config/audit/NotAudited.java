package com.eudext.erp.config.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AUD-1 opts an {@code @Entity} out of the generic {@code AuditingInterceptor}
 * in the {@code audit} module. Entities that are already their own
 * audit-shaped record (e.g. IAM's {@code PasswordHistory}), pure
 * session/technical bookkeeping rather than transactional or master data,
 * or otherwise covered by a dedicated event listener should carry this
 * rather than accumulate noisy or redundant audit_log rows.
 *
 * <p>Lives in {@code config} — the shared, dependency-free, {@code OPEN}
 * module (ARCH-1) — rather than in {@code audit} itself: every domain
 * module that has an entity to opt out needs to depend on this annotation,
 * while the {@code audit} module also needs to depend on {@code iam}'s
 * published events and {@code PermissionApi}. Putting the annotation in
 * {@code audit} would make that a two-way module dependency, which Spring
 * Modulith's cycle check rejects outright.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NotAudited {}
