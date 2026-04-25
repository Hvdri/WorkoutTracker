package com.workout_tracker.backend.dto.user;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
    @Size(max = 500) String bio,
    @Size(max = 100) String fitnessGoal,
    @DecimalMin("50.0") @DecimalMax("300.0") Double heightCm,
    @DecimalMin("20.0") @DecimalMax("500.0") Double weightKg,
    @Size(max = 20) String gender
) {}
