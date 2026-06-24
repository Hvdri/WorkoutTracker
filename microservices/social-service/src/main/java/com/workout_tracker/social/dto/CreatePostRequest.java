package com.workout_tracker.social.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
    @NotNull Long workoutLogId,
    @Size(max = 300) String caption
) {}
