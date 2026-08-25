package com.eudext.erp.iam;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * IAM-3: a permission is a {@code module:entity:action} triple, e.g.
 * {@code finance:journal-entry:submit}. There is deliberately no backing
 * catalog table (see V7 migration comment) — this validates shape only, so
 * modules that don't exist yet in Phase 0 don't block RBAC from landing.
 */
public record PermissionCode(String module, String entity, String action) {

    private static final Pattern SEGMENT = Pattern.compile("[a-z][a-z0-9-]*");

    public PermissionCode {
        requireValidSegment(module, "module");
        requireValidSegment(entity, "entity");
        requireValidSegment(action, "action");
    }

    public static PermissionCode parse(String code) {
        Objects.requireNonNull(code, "code");
        String[] parts = code.split(":", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "Permission code must be `module:entity:action`, got: " + code);
        }
        return new PermissionCode(parts[0], parts[1], parts[2]);
    }

    /** The `module:entity` half, used to key field-level permissions. */
    public String entityCode() {
        return module + ":" + entity;
    }

    @Override
    public String toString() {
        return module + ":" + entity + ":" + action;
    }

    private static void requireValidSegment(String value, String name) {
        if (value == null || !SEGMENT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Permission " + name + " must match " + SEGMENT.pattern() + ", got: " + value);
        }
    }
}
