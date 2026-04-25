package com.workout_tracker.backend.dto.workout;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSplitRequest(
    @NotBlank @Size(max = 50) String name
) {}
