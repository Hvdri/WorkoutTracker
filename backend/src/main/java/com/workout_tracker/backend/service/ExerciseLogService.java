package com.workout_tracker.backend.service;

import com.workout_tracker.backend.dto.workout.CreateExerciseLogRequest;
import com.workout_tracker.backend.dto.workout.ExerciseLogDto;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.Exercise;
import com.workout_tracker.backend.model.ExerciseLog;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.WorkoutLog;
import com.workout_tracker.backend.repository.ExerciseLogRepository;
import com.workout_tracker.backend.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseLogService {

    private final ExerciseLogRepository exerciseLogRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutLogService workoutLogService;

    // Business rule #6: ExerciseLog can only be added to a WorkoutLog owned by the user.
    @Transactional
    public ExerciseLogDto addExerciseLog(Long workoutLogId, User user, CreateExerciseLogRequest request) {
        WorkoutLog workoutLog = workoutLogService.loadOwnedLog(workoutLogId, user);

        Exercise exercise = exerciseRepository.findById(request.exerciseId())
                .orElseThrow(() -> ResourceNotFoundException.of("Exercise", request.exerciseId()));

        ExerciseLog ex = ExerciseLog.builder()
                .workoutLog(workoutLog)
                .exercise(exercise)
                .notes(request.notes())
                .build();

        ExerciseLog saved = exerciseLogRepository.save(ex);
        // Keep the parent's in-memory collection consistent. Without this, a subsequent
        // read of WorkoutLog within the same persistence context returns it with its
        // original (empty) exerciseLogs list — matters in tests and any flow that
        // writes then reads the same WorkoutLog before transaction commit.
        workoutLog.getExerciseLogs().add(saved);

        log.info("ExerciseLog {} added to WorkoutLog {} by user {}",
                saved.getId(), workoutLogId, user.getUsername());
        return WorkoutMapper.toExerciseLogDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ExerciseLogDto> getExerciseLogs(Long workoutLogId, User user) {
        WorkoutLog workoutLog = workoutLogService.loadOwnedLog(workoutLogId, user);
        return exerciseLogRepository.findByWorkoutLog(workoutLog).stream()
                .map(WorkoutMapper::toExerciseLogDto)
                .toList();
    }

    @Transactional
    public void deleteExerciseLog(Long workoutLogId, Long id, User user) {
        ExerciseLog ex = loadOwnedExerciseLog(workoutLogId, id, user);
        exerciseLogRepository.delete(ex);
        log.info("ExerciseLog {} deleted by user {}", id, user.getUsername());
    }

    // Loads an exercise log, confirms the user owns the workout log it belongs to,
    // and asserts the URL hierarchy (workoutLogId param matches the parent).
    // Both miss-and-wrong-owner cases surface as 404 (ProjectContext rule #2).
    // Package-private so SetLogService can reuse the ownership chain.
    ExerciseLog loadOwnedExerciseLog(Long workoutLogId, Long id, User user) {
        ExerciseLog ex = exerciseLogRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ExerciseLog", id));
        if (!ex.getWorkoutLog().getUser().getId().equals(user.getId())
                || !ex.getWorkoutLog().getId().equals(workoutLogId)) {
            log.warn("User {} attempted to access ExerciseLog {} they do not own (or wrong path workoutLogId={})",
                    user.getUsername(), id, workoutLogId);
            throw ResourceNotFoundException.of("ExerciseLog", id);
        }
        return ex;
    }
}
