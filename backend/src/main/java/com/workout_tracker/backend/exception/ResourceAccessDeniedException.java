package com.workout_tracker.backend.exception;

// Thrown when an authenticated user attempts to act on a resource they don't own.
// Named with "Resource" prefix to avoid clashing with Spring Security's AccessDeniedException.
public class ResourceAccessDeniedException extends RuntimeException {

    public ResourceAccessDeniedException(String message) {
        super(message);
    }
}
