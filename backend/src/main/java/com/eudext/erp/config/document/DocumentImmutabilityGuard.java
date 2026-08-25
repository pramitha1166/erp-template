package com.eudext.erp.config.document;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * ARCH-4: "a submitted document is immutable except for fields explicitly
 * designated amendable." Usage is snapshot-then-assert, since the natural
 * way to mutate a JPA entity is in place:
 *
 * <pre>{@code
 * Map<String, Object> before = DocumentImmutabilityGuard.snapshot(document);
 * document.setNote(newNote);
 * DocumentImmutabilityGuard.assertNoDisallowedChanges(document, before);
 * }</pre>
 *
 * <p>A {@code DRAFT} document is always freely editable; the guard is a
 * no-op there.
 */
public final class DocumentImmutabilityGuard {

    /** Fields the ARCH-4 state machine itself is allowed to change post-submit. */
    private static final Set<String> FRAMEWORK_MANAGED_FIELDS =
            Set.of("docStatus", "modifiedBy", "modifiedAt", "version");

    private DocumentImmutabilityGuard() {}

    public static Map<String, Object> snapshot(Document document) {
        Objects.requireNonNull(document, "document");
        Map<String, Object> values = new LinkedHashMap<>();
        for (Field field : allFields(document.getClass())) {
            values.put(field.getName(), read(field, document));
        }
        return values;
    }

    public static void assertNoDisallowedChanges(Document document, Map<String, Object> before) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(before, "before");
        if (statusAtSnapshotTime(before) == DocStatus.DRAFT) {
            return;
        }
        for (Field field : allFields(document.getClass())) {
            Object oldValue = before.get(field.getName());
            Object newValue = read(field, document);
            if (Objects.equals(oldValue, newValue)) {
                continue;
            }
            boolean allowed = FRAMEWORK_MANAGED_FIELDS.contains(field.getName())
                    || field.isAnnotationPresent(Amendable.class);
            if (!allowed) {
                throw new DocumentImmutableException(document.getId(), field.getName());
            }
        }
    }

    private static DocStatus statusAtSnapshotTime(Map<String, Object> before) {
        return (DocStatus) before.get("docStatus");
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    private static Object read(Field field, Document document) {
        try {
            return field.get(document);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read field " + field.getName() + " for immutability check", e);
        }
    }
}
