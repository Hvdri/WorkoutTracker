package com.workout_tracker.notification.dto;

import java.time.LocalDateTime;

public record NotificationDto(
    String id,
    Long userId,
    String type,
    String message,
    Long workoutLogId,
    LocalDateTime createdAt,
    boolean read
) {}
