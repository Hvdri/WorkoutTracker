package com.workout_tracker.backend.service;

import com.workout_tracker.backend.dto.workout.CreateExerciseRequest;
import com.workout_tracker.backend.dto.workout.ExerciseDto;
import com.workout_tracker.backend.exception.BusinessRuleViolationException;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.Exercise;
import com.workout_tracker.backend.model.enums.MuscleGroup;
import com.workout_tracker.backend.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    // Single read entry-point: caller can pass nothing, a muscle group, a name fragment,
    // or both; filters combine when both are present.
    @Transactional(readOnly = true)
    public Page<ExerciseDto> findExercises(MuscleGroup muscleGroup, String name, Pageable pageable) {
        boolean hasName = name != null && !name.isBlank();
        Page<Exercise> page;
        if (muscleGroup != null && hasName) {
            page = exerciseRepository.findByMuscleGroupAndNameContainingIgnoreCase(muscleGroup, name, pageable);
        } else if (muscleGroup != null) {
            page = exerciseRepository.findByMuscleGroup(muscleGroup, pageable);
        } else if (hasName) {
            page = exerciseRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            page = exerciseRepository.findAll(pageable);
        }
        return page.map(ExerciseService::toDto);
    }

    @Transactional(readOnly = true)
    public ExerciseDto getExerciseById(Long id) {
        return exerciseRepository.findById(id)
                .map(ExerciseService::toDto)
                .orElseThrow(() -> ResourceNotFoundException.of("Exercise", id));
    }

    @Transactional
    public ExerciseDto createExercise(CreateExerciseRequest request) {
        if (exerciseRepository.existsByNameIgnoreCase(request.name())) {
            log.warn("Refused duplicate Exercise name: {}", request.name());
            throw new BusinessRuleViolationException(
                    "Exercise with name '" + request.name() + "' already exists");
        }

        Exercise exercise = Exercise.builder()
                .name(request.name())
                .description(request.description())
                .muscleGroup(request.muscleGroup())
                .imageUrl(request.imageUrl())
                .build();

        Exercise saved = exerciseRepository.save(exercise);
        log.info("Exercise {} created (id={})", saved.getName(), saved.getId());
        return toDto(saved);
    }

    @Transactional
    public ExerciseDto updateExercise(Long id, CreateExerciseRequest request) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Exercise", id));

        // Only block on duplicate if the name actually changed (case-insensitive).
        if (!exercise.getName().equalsIgnoreCase(request.name())
                && exerciseRepository.existsByNameIgnoreCase(request.name())) {
            log.warn("Refused rename of Exercise {} to duplicate name {}", id, request.name());
            throw new BusinessRuleViolationException(
                    "Exercise with name '" + request.name() + "' already exists");
        }

        exercise.setName(request.name());
        exercise.setDescription(request.description());
        exercise.setMuscleGroup(request.muscleGroup());
        exercise.setImageUrl(request.imageUrl());
        // No explicit save() — the entity is managed in this @Transactional method,
        // so Hibernate dirty-checks and flushes on commit.

        log.info("Exercise {} updated", id);
        return toDto(exercise);
    }

    @Transactional
    public void deleteExercise(Long id) {
        if (!exerciseRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Exercise", id);
        }
        exerciseRepository.deleteById(id);
        log.info("Exercise {} deleted", id);
    }

    private static ExerciseDto toDto(Exercise e) {
        return new ExerciseDto(
                e.getId(),
                e.getName(),
                e.getDescription(),
                e.getMuscleGroup().name(),
                e.getImageUrl()
        );
    }
}
