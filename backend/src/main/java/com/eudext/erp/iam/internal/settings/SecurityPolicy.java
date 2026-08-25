package com.eudext.erp.iam.internal.settings;

/**
 * IAM-8 / IAM-9: a tenant's effective security policy — password
 * length/complexity/history/expiry and session idle timeout. Values mirror
 * the column defaults in {@code V6__iam_security_settings.sql} so a tenant
 * with no row and a tenant with a row that hasn't customized anything
 * behave identically.
 */
public record SecurityPolicy(
        int idleTimeoutMinutes,
        int minLength,
        boolean requireUpper,
        boolean requireLower,
        boolean requireDigit,
        boolean requireSymbol,
        int historyCount,
        Integer expiryDays) {

    public static SecurityPolicy defaults() {
        return new SecurityPolicy(30, 10, true, true, true, false, 3, null);
    }
}
