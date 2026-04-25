package com.workout_tracker.backend.dto.workout;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateWorkoutLogRequest(
    @NotNull @PastOrPresent LocalDate date,
    @NotNull Long templateId,
    String photoUrl,
    @Size(max = 500) String notes
) {}
