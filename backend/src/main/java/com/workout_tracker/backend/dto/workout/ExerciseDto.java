package com.workout_tracker.backend.dto.workout;

public record ExerciseDto(
    Long id,
    String name,
    String description,
    String muscleGroup,
    String imageUrl
) {}
