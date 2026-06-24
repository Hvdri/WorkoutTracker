package com.workout_tracker.social.dto;

import java.time.LocalDateTime;

// Same wire format as the monolith's PostDto so the frontend types don't have to change
// when calls get routed to social-service via the Vite proxy.
public record PostDto(
    Long id,
    Long userId,
    String username,
    Long workoutLogId,
    String templateName,
    String caption,
    LocalDateTime createdAt
) {}
