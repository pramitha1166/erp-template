package com.eudext.erp.admin.internal.web;

import com.eudext.erp.admin.internal.entitlement.EntitlementBoundExceededException;
import com.eudext.erp.config.tenancy.TenantSuspendedException;
import com.eudext.erp.iam.AuthenticationFailedException;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates {@code admin}'s internal exceptions into the REST layer's error responses — mirrors {@code IamExceptionHandler}. */
@RestControllerAdvice(basePackages = "com.eudext.erp.admin")
public class AdminExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AdminApiError> accessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(AdminApiError.of(e.getMessage()));
    }

    @ExceptionHandler(TenantSuspendedException.class)
    public ResponseEntity<AdminApiError> tenantSuspended(TenantSuspendedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(AdminApiError.of(e.getMessage()));
    }

    /** ADM-1 / ADM-5: admin-realm login — same generic-message treatment as {@code IamExceptionHandler}'s. */
    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<AdminApiError> authenticationFailed(AuthenticationFailedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AdminApiError.of(e.getMessage()));
    }

    @ExceptionHandler(EntitlementBoundExceededException.class)
    public ResponseEntity<AdminApiError> entitlementBoundExceeded(EntitlementBoundExceededException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(AdminApiError.of(e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<AdminApiError> notFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(AdminApiError.of(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<AdminApiError> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(AdminApiError.of(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AdminApiError> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AdminApiError.of(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AdminApiError> validationFailed(MethodArgumentNotValidException e) {
        var details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AdminApiError.of("Validation failed", details));
    }
}
