package com.workout_tracker.backend.dto.workout;

import com.workout_tracker.backend.model.enums.MuscleGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExerciseRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 1000) String description,
    @NotNull MuscleGroup muscleGroup,
    String imageUrl
) {}
