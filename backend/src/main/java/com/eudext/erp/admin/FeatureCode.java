package com.eudext.erp.admin;

import java.util.regex.Pattern;

/**
 * BRD-12 / ADM-1: a feature/module entitlement code, e.g. {@code MOD-LK},
 * {@code INVENTORY}, {@code PAYROLL}. No backing catalog table yet — same
 * deliberate looseness as {@code iam.PermissionCode} (see its javadoc):
 * modules that don't exist yet in Phase 0 shouldn't block entitlements
 * from being modeled.
 */
public record FeatureCode(String value) {

    private static final Pattern SHAPE = Pattern.compile("[A-Z][A-Z0-9-]*");

    public FeatureCode {
        if (value == null || !SHAPE.matcher(value).matches()) {
            throw new IllegalArgumentException("Feature code must match " + SHAPE.pattern() + ", got: " + value);
        }
    }
}
