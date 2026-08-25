package com.eudext.erp.audit.internal.write;

import com.eudext.erp.config.audit.AuditRedacted;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * AUD-2: pure diffing logic pulled out of {@code AuditingInterceptor} so it
 * can be unit-tested without a Hibernate session. Fields annotated
 * {@link AuditRedacted} still appear in the diff (so it's visible that the
 * field changed) but with their value replaced by {@link #REDACTED_MARKER}.
 */
final class PropertyDiff {

    static final String REDACTED_MARKER = "***REDACTED***";

    private PropertyDiff() {}

    /** All properties, for an INSERT — everything is "new". */
    static Map<String, Object> allAsNew(Class<?> entityType, String[] propertyNames, Object[] state) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < propertyNames.length; i++) {
            values.put(propertyNames[i], redactedIfNeeded(entityType, propertyNames[i], state[i]));
        }
        return values;
    }

    /** All properties, for a DELETE — everything is "old". */
    static Map<String, Object> allAsOld(Class<?> entityType, String[] propertyNames, Object[] state) {
        return allAsNew(entityType, propertyNames, state);
    }

    /**
     * Only the properties that actually changed between {@code previousState}
     * and {@code currentState}, for an UPDATE. Returns two same-keyed maps
     * (old/new) so a caller can tell which fields changed and what each side
     * of the change was.
     */
    static Changed changed(Class<?> entityType, String[] propertyNames, Object[] previousState, Object[] currentState) {
        Map<String, Object> oldValues = new LinkedHashMap<>();
        Map<String, Object> newValues = new LinkedHashMap<>();
        for (int i = 0; i < propertyNames.length; i++) {
            Object before = previousState == null ? null : previousState[i];
            Object after = currentState[i];
            if (Objects.equals(before, after)) {
                continue;
            }
            oldValues.put(propertyNames[i], redactedIfNeeded(entityType, propertyNames[i], before));
            newValues.put(propertyNames[i], redactedIfNeeded(entityType, propertyNames[i], after));
        }
        return new Changed(oldValues, newValues);
    }

    record Changed(Map<String, Object> oldValues, Map<String, Object> newValues) {
        boolean isEmpty() {
            return oldValues.isEmpty();
        }
    }

    private static Object redactedIfNeeded(Class<?> entityType, String propertyName, Object value) {
        return isRedacted(entityType, propertyName) ? REDACTED_MARKER : value;
    }

    private static boolean isRedacted(Class<?> entityType, String propertyName) {
        for (Class<?> current = entityType; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(propertyName);
                if (field.isAnnotationPresent(AuditRedacted.class)) {
                    return true;
                }
                return false;
            } catch (NoSuchFieldException ignored) {
                // keep walking up to the superclass
            }
        }
        return false;
    }
}
