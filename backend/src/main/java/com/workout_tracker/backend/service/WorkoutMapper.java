package com.workout_tracker.backend.service;

import com.workout_tracker.backend.dto.workout.ExerciseLogDto;
import com.workout_tracker.backend.dto.workout.ExerciseTemplateDto;
import com.workout_tracker.backend.dto.workout.SetLogDto;
import com.workout_tracker.backend.dto.workout.WorkoutLogDto;
import com.workout_tracker.backend.dto.workout.WorkoutSplitDto;
import com.workout_tracker.backend.dto.workout.WorkoutTemplateDto;
import com.workout_tracker.backend.model.ExerciseLog;
import com.workout_tracker.backend.model.ExerciseTemplate;
import com.workout_tracker.backend.model.SetLog;
import com.workout_tracker.backend.model.WorkoutLog;
import com.workout_tracker.backend.model.WorkoutSplit;
import com.workout_tracker.backend.model.WorkoutTemplate;

import java.util.Comparator;
import java.util.List;

// Static mappers for workout-domain entities → DTOs. Kept in one place so each service
// doesn't repeat the same toXxxDto() snippet, and so the relation traversal happens
// inside the @Transactional service call where lazy associations can still be loaded.
final class WorkoutMapper {

    private WorkoutMapper() {}

    static WorkoutSplitDto toSplitDto(WorkoutSplit split) {
        List<WorkoutTemplateDto> templates = split.getTemplates().stream()
                .sorted(Comparator.comparingInt(WorkoutTemplate::getOrderIndex))
                .map(WorkoutMapper::toTemplateDto)
                .toList();
        return new WorkoutSplitDto(
                split.getId(),
                split.getName(),
                split.isActive(),
                split.getCreatedAt(),
                templates
        );
    }

    static WorkoutTemplateDto toTemplateDto(WorkoutTemplate template) {
        List<ExerciseTemplateDto> exercises = template.getExerciseTemplates().stream()
                .sorted(Comparator.comparingInt(ExerciseTemplate::getOrderIndex))
                .map(WorkoutMapper::toExerciseTemplateDto)
                .toList();
        return new WorkoutTemplateDto(
                template.getId(),
                template.getName(),
                template.getOrderIndex(),
                exercises
        );
    }

    static ExerciseTemplateDto toExerciseTemplateDto(ExerciseTemplate et) {
        return new ExerciseTemplateDto(
                et.getId(),
                et.getExercise().getId(),
                et.getExercise().getName(),
                et.getExercise().getMuscleGroup().name(),
                et.getTargetSets(),
                et.getTargetReps(),
                et.getOrderIndex()
        );
    }

    static WorkoutLogDto toWorkoutLogDto(WorkoutLog log) {
        // Sort by id so the UI sees a stable order. ExerciseLog has no orderIndex
        // field (and adding one is a schema change); insertion order = id order
        // is good enough.
        List<ExerciseLogDto> exerciseLogs = log.getExerciseLogs().stream()
                .sorted(Comparator.comparingLong(ExerciseLog::getId))
                .map(WorkoutMapper::toExerciseLogDto)
                .toList();
        return new WorkoutLogDto(
                log.getId(),
                log.getDate(),
                log.getPhotoUrl(),
                log.getStatus().name(),
                log.getNotes(),
                log.getTemplate().getId(),
                log.getTemplate().getName(),
                exerciseLogs,
                log.getCreatedAt()
        );
    }

    static ExerciseLogDto toExerciseLogDto(ExerciseLog ex) {
        List<SetLogDto> sets = ex.getSets().stream()
                .sorted(Comparator.comparingInt(SetLog::getSetNumber))
                .map(WorkoutMapper::toSetLogDto)
                .toList();
        return new ExerciseLogDto(
                ex.getId(),
                ex.getExercise().getId(),
                ex.getExercise().getName(),
                ex.getExercise().getMuscleGroup().name(),
                ex.getNotes(),
                sets
        );
    }

    static SetLogDto toSetLogDto(SetLog set) {
        return new SetLogDto(
                set.getId(),
                set.getSetNumber(),
                set.getWeightKg(),
                set.getReps(),
                set.getRpe()
        );
    }
}
