package com.eudext.erp.config.document;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field on a {@link Document} subclass as one of the "designated
 * amendable fields" ARCH-4 allows to change after a document reaches
 * {@link DocStatus#SUBMITTED}. Every other field is frozen by
 * {@link DocumentImmutabilityGuard} once the document leaves {@code DRAFT}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Amendable {}
