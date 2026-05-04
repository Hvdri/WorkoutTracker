package com.workout_tracker.backend.dto.user;

import com.workout_tracker.backend.model.enums.Gender;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// PATCH semantics: null = leave the field unchanged. To prevent the silent-clear bug
// where {"fitnessGoal":""} wipes the field, the @Pattern below requires at least one
// non-whitespace character. @Pattern is null-safe, so null still means "unchanged".
// gender is a Gender enum — Jackson rejects empty strings and unknown values at
// deserialization, so no extra validator is needed.
public record UserProfileUpdateRequest(
    @Size(max = 500) String bio,

    @Size(max = 100)
    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    String fitnessGoal,

    @DecimalMin("50.0") @DecimalMax("300.0") Double heightCm,
    @DecimalMin("20.0") @DecimalMax("500.0") Double weightKg,
    Gender gender
) {}
