package com.workout_tracker.backend.exception;

// Thrown when a domain invariant is violated:
// duplicate active split, self-follow, completing a log that's not yours, etc.
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
