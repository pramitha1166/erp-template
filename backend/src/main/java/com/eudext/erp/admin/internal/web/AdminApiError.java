package com.eudext.erp.admin.internal.web;

import java.util.List;

/** Mirrors {@code iam.internal.web.ApiError} — kept as a separate type since {@code admin} cannot depend on IAM's internal web package (ARCH-1). */
public record AdminApiError(String message, List<String> details) {

    public static AdminApiError of(String message) {
        return new AdminApiError(message, List.of());
    }

    public static AdminApiError of(String message, List<String> details) {
        return new AdminApiError(message, details);
    }
}
