package com.workout_tracker.backend.dto.workout;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExerciseLogRequest(
    @NotNull Long exerciseId,
    @Size(max = 500) String notes
) {}
