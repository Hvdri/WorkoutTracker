package com.workout_tracker.backend.dto.workout;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddExerciseToTemplateRequest(
    @NotNull Long exerciseId,
    @Min(1) Integer targetSets,
    @Min(1) Integer targetReps,
    int orderIndex
) {}
