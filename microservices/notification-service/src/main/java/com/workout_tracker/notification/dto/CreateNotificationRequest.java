package com.workout_tracker.notification.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Payload from monolith → /internal/notifications when a workout log is completed.
// type is freeform so future kinds (POST_LIKED, etc) don't need a schema bump.
public record CreateNotificationRequest(
    @NotNull Long userId,
    @NotNull @Size(max = 32) String type,
    @NotNull @Size(max = 500) String message,
    Long workoutLogId
) {}
