package com.workout_tracker.backend.dto.workout;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateSetLogRequest(
    @Min(1) int setNumber,
    @NotNull @DecimalMin("0.0") BigDecimal weightKg,
    @Min(1) @Max(100) int reps,
    @Min(1) @Max(10) Integer rpe
) {}
