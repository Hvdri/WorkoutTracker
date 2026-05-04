package com.workout_tracker.backend.dto.user;

import com.workout_tracker.backend.model.enums.Gender;

// heightCm is Double (not Integer as in the spec) to match the existing UserProfile entity,
// which uses Double. Changing the entity now would require a migration; revisit if the spec
// is intentional about integer cm.
public record UserProfileDto(
    Long userId,
    String username,
    String displayName,
    String bio,
    String fitnessGoal,
    Double heightCm,
    Double weightKg,
    Gender gender
) {}
