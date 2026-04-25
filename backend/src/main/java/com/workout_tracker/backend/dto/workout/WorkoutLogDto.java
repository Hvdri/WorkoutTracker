package com.workout_tracker.backend.dto.workout;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record WorkoutLogDto(
    Long id,
    LocalDate date,
    String photoUrl,
    String status,
    String notes,
    Long templateId,
    String templateName,
    List<ExerciseLogDto> exerciseLogs,
    LocalDateTime createdAt
) {}
