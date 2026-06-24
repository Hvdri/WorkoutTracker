package com.workout_tracker.social.exception;

import java.time.LocalDateTime;

// Mirrors the monolith's error envelope shape so the frontend's error-message extractor
// keeps working across the proxy boundary.
public record ErrorResponse(
    int status,
    String error,
    String message,
    LocalDateTime timestamp
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now());
    }
}
