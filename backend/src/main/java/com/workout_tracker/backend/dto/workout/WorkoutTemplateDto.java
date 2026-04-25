package com.workout_tracker.backend.dto.workout;

import java.util.List;

public record WorkoutTemplateDto(
    Long id,
    String name,
    int orderIndex,
    List<ExerciseTemplateDto> exercises
) {}
