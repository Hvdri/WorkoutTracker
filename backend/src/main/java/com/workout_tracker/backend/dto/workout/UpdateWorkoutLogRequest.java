package com.workout_tracker.backend.dto.workout;

import com.workout_tracker.backend.model.enums.WorkoutStatus;
import jakarta.validation.constraints.Size;

public record UpdateWorkoutLogRequest(
    String photoUrl,
    @Size(max = 500) String notes,
    WorkoutStatus status
) {}
