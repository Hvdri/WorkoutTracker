package com.workout_tracker.backend.dto.internal;

// Compact projection of WorkoutLog exposed only via /internal/ — used by social-service
// to validate post-creation rules without owning the WorkoutLog entity itself.
public record LogSummaryDto(
    Long id,
    Long ownerId,
    String status,
    String templateName
) {}
