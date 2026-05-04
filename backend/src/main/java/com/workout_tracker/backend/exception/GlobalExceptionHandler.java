package com.workout_tracker.backend.exception;

import com.workout_tracker.backend.dto.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

// Extending ResponseEntityExceptionHandler picks up Spring MVC's standard
// mappings (malformed JSON → 400, type mismatch → 400, unsupported media
// type → 415, method not allowed → 405, etc.). Without it those would fall
// through to the @ExceptionHandler(Exception.class) catch-all and surface
// as 500s.
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

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

    // DB constraint backstop — typically a uniqueness or referential constraint.
    // Service-layer checks should preempt these (see SetLogService duplicate
    // setNumber check), but a concurrent-insert race can still slip past an
    // in-memory check before either request commits. Without this handler the
    // exception falls through to the catch-all and surfaces as a 500.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT,
                        "Resource conflict — the operation violates a uniqueness or referential constraint"));
    }

    // Catch-all — log the unexpected error but don't leak stack traces to the client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"));
    }

    // @Valid failed — keep the field-error map shape the frontend already renders
    // inline next to inputs. Override the base class hook so we keep responsibility
    // for this one type rather than registering a competing @ExceptionHandler.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ErrorResponse body = ErrorResponse.withFieldErrors(
                HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
        return new ResponseEntity<>(body, headers, HttpStatus.BAD_REQUEST);
    }

    // ResponseEntityExceptionHandler defaults to a null body or RFC 7807 ProblemDetail.
    // Convert those to our canonical ErrorResponse so every error response — domain
    // or framework — looks the same to clients.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        if (body instanceof ErrorResponse) {
            return new ResponseEntity<>(body, headers, statusCode);
        }
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        log.warn("{} → {}: {}", ex.getClass().getSimpleName(), status, ex.getMessage());
        String message = ex.getMessage() != null ? ex.getMessage() : status.getReasonPhrase();
        ErrorResponse error = ErrorResponse.of(status, message);
        return new ResponseEntity<>(error, headers, statusCode);
    }
}
