package com.workout_tracker.backend.dto.social;

import java.time.LocalDateTime;

public record PostDto(
    Long id,
    Long userId,
    String username,
    Long workoutLogId,
    String templateName,
    String caption,
    LocalDateTime createdAt
) {}
