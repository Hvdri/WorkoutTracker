package com.workout_tracker.backend.exception;

import com.workout_tracker.backend.dto.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// @RestControllerAdvice means: "intercept exceptions from any @RestController and run these handlers".
// Instead of Spring returning an ugly 500 HTML error page, we return clean JSON.
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Registration conflict — username or email already taken
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage()));
    }

    // Domain rule violation (e.g. self-follow, no active split, completing non-COMPLETED log)
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage()));
    }

    // Resource missing — no entity with that id, or not visible to this user
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    // Authenticated, but accessing someone else's resource — our domain version
    @ExceptionHandler(ResourceAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccessDenied(ResourceAccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN, ex.getMessage()));
    }

    // Spring Security's AccessDeniedException (e.g. @PreAuthorize blocked) — without
    // this handler Spring would render an HTML error page instead of JSON.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSpringAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN, "Access denied"));
    }

    // Wrong username or password — covers BadCredentialsException and other auth failures.
    // Generic message intentional: don't reveal whether username or password was wrong
    // (prevents username enumeration attacks).
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Failed login attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
    }

    // @Valid failed — keeps the standard envelope and adds a field map under `errors`
    // so the frontend can render messages inline next to each input.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.withFieldErrors(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors));
    }

    // Catch-all — log the unexpected error but don't leak stack traces to the client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"));
    }
}
