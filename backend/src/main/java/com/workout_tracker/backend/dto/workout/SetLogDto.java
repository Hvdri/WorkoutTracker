package com.workout_tracker.backend.dto.workout;

import java.math.BigDecimal;

public record SetLogDto(
    Long id,
    int setNumber,
    BigDecimal weightKg,
    int reps,
    Integer rpe
) {}
