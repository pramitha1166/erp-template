package com.eudext.erp.audit.internal.write;

import com.eudext.erp.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AUD-2: resolves the client IP and request id for whichever HTTP request
 * is currently being processed on this thread, for the generic
 * {@code AuditingInterceptor}. Returns {@code null} for both outside a
 * request (scheduled jobs, startup, tests) — there simply isn't one.
 *
 * <p>Reuses {@link CorrelationIdFilter}'s MDC entry as the request id
 * rather than introducing a second id, and duplicates its small
 * X-Forwarded-For parsing rather than depending on IAM's
 * {@code AuthController} for it — {@code config} is the shared,
 * dependency-free module (ARCH-1) and must not import from a domain
 * module.
 */
final class AuditRequestContext {

    private AuditRequestContext() {}

    static String currentIpAddress() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor != null ? forwardedFor.split(",")[0].trim() : request.getRemoteAddr();
    }

    static String currentRequestId() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servletAttributes ? servletAttributes.getRequest() : null;
    }
}
