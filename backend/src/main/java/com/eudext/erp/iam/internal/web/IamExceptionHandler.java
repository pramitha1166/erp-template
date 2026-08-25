package com.eudext.erp.iam.internal.web;

import com.eudext.erp.iam.internal.auth.AuthenticationFailedException;
import com.eudext.erp.iam.internal.password.PasswordPolicyViolationException;
import com.eudext.erp.iam.internal.rbac.TotpRequiredException;
import com.eudext.erp.iam.internal.session.InvalidRefreshTokenException;
import com.eudext.erp.iam.internal.session.RefreshTokenReuseDetectedException;
import com.eudext.erp.iam.internal.sod.SegregationOfDutiesViolationException;
import com.eudext.erp.iam.internal.totp.InvalidTotpCodeException;
import com.eudext.erp.iam.internal.user.UserAlreadyExistsException;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates IAM's internal exceptions into the REST layer's error responses. */
@RestControllerAdvice(basePackages = "com.eudext.erp.iam")
public class IamExceptionHandler {

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiError> authenticationFailed(AuthenticationFailedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.of(e.getMessage()));
    }

    @ExceptionHandler({InvalidRefreshTokenException.class, RefreshTokenReuseDetectedException.class})
    public ResponseEntity<ApiError> invalidSession(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.of(e.getMessage()));
    }

    @ExceptionHandler(InvalidTotpCodeException.class)
    public ResponseEntity<ApiError> invalidTotp(InvalidTotpCodeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.of(e.getMessage()));
    }

    @ExceptionHandler(PasswordPolicyViolationException.class)
    public ResponseEntity<ApiError> passwordPolicy(PasswordPolicyViolationException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError.of(e.getMessage(), e.getViolations()));
    }

    @ExceptionHandler(SegregationOfDutiesViolationException.class)
    public ResponseEntity<ApiError> sodViolation(SegregationOfDutiesViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(e.getMessage()));
    }

    @ExceptionHandler(TotpRequiredException.class)
    public ResponseEntity<ApiError> totpRequired(TotpRequiredException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(e.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiError> userExists(UserAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> notFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.of(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validationFailed(MethodArgumentNotValidException e) {
        var details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.of("Validation failed", details));
    }
}
