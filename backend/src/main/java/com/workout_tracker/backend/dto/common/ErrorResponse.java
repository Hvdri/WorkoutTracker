package com.workout_tracker.backend.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

// `errors` is omitted from JSON when null (i.e. for non-validation errors),
// so single-message errors stay clean while validation responses gain a field map.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    int status,
    String error,
    String message,
    LocalDateTime timestamp,
    Map<String, String> errors
) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, LocalDateTime.now(), null);
    }

    public static ErrorResponse withFieldErrors(HttpStatus status, String message, Map<String, String> errors) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, LocalDateTime.now(), errors);
    }
}
