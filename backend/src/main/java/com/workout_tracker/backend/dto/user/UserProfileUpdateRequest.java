package com.workout_tracker.backend.dto.user;

import com.workout_tracker.backend.model.enums.Gender;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

// PUT-style update: every field on the request overwrites the stored value, so
// null means "clear this field". The frontend always submits the full current form
// state, so unchanged fields arrive at their existing values and the user can
// actually clear bio/goal/height/weight/gender from the UI.
// gender is a Gender enum — Jackson rejects empty strings and unknown values at
// deserialization, so no extra validator is needed.
public record UserProfileUpdateRequest(
    @Size(max = 500) String bio,
    @Size(max = 100) String fitnessGoal,
    @DecimalMin("50.0") @DecimalMax("300.0") Double heightCm,
    @DecimalMin("20.0") @DecimalMax("500.0") Double weightKg,
    Gender gender
) {}
