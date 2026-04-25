package com.workout_tracker.backend.dto.workout;

import java.util.List;

public record ExerciseLogDto(
    Long id,
    Long exerciseId,
    String exerciseName,
    String muscleGroup,
    String notes,
    List<SetLogDto> sets
) {}
