package com.workout_tracker.backend.dto.workout;

import java.time.LocalDateTime;
import java.util.List;

public record WorkoutSplitDto(
    Long id,
    String name,
    boolean isActive,
    LocalDateTime createdAt,
    List<WorkoutTemplateDto> templates
) {}
