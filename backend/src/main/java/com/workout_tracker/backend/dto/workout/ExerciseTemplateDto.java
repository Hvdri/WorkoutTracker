package com.workout_tracker.backend.dto.workout;

public record ExerciseTemplateDto(
    Long id,
    Long exerciseId,
    String exerciseName,
    String muscleGroup,
    Integer targetSets,
    Integer targetReps,
    int orderIndex
) {}
