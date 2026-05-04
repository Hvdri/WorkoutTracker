package com.workout_tracker.backend.dto.workout;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record UpdateSetLogRequest(
    @DecimalMin("0.0") BigDecimal weightKg,
    @Min(1) @Max(100) Integer reps,
    @Min(1) @Max(10) Integer rpe
) {}
