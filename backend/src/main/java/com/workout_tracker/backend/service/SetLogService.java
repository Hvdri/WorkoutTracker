package com.workout_tracker.backend.service;

import com.workout_tracker.backend.dto.workout.CreateSetLogRequest;
import com.workout_tracker.backend.dto.workout.SetLogDto;
import com.workout_tracker.backend.dto.workout.UpdateSetLogRequest;
import com.workout_tracker.backend.exception.BusinessRuleViolationException;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.ExerciseLog;
import com.workout_tracker.backend.model.SetLog;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.repository.SetLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetLogService {

    private final SetLogRepository setLogRepository;
    private final ExerciseLogService exerciseLogService;

    @Transactional
    public SetLogDto addSet(Long workoutLogId, Long exerciseLogId, User user, CreateSetLogRequest request) {
        ExerciseLog parent = exerciseLogService.loadOwnedExerciseLog(workoutLogId, exerciseLogId, user);

        // setNumber must be unique within an ExerciseLog. The DB carries a unique
        // constraint as a backstop; this check produces a clean 409 instead of a
        // 500 from a constraint violation.
        boolean duplicate = parent.getSets().stream()
                .anyMatch(s -> s.getSetNumber() == request.setNumber());
        if (duplicate) {
            log.warn("User {} attempted to add SetLog with duplicate setNumber {} to ExerciseLog {}",
                    user.getUsername(), request.setNumber(), exerciseLogId);
            throw new BusinessRuleViolationException(
                    "Set number " + request.setNumber() + " already exists for this exercise");
        }

        SetLog set = SetLog.builder()
                .exerciseLog(parent)
                .setNumber(request.setNumber())
                .weightKg(request.weightKg())
                .reps(request.reps())
                .rpe(request.rpe())
                .build();

        SetLog saved = setLogRepository.save(set);
        // Keep the parent's in-memory collection consistent (same reason as in
        // ExerciseLogService.addExerciseLog).
        parent.getSets().add(saved);

        log.info("SetLog {} added to ExerciseLog {} by user {}",
                saved.getId(), exerciseLogId, user.getUsername());
        return WorkoutMapper.toSetLogDto(saved);
    }

    @Transactional
    public SetLogDto updateSet(Long workoutLogId, Long exerciseLogId, Long setId, User user,
                               UpdateSetLogRequest request) {
        SetLog set = loadOwnedSet(workoutLogId, exerciseLogId, setId, user);
        if (request.weightKg() != null) set.setWeightKg(request.weightKg());
        if (request.reps() != null) set.setReps(request.reps());
        if (request.rpe() != null) set.setRpe(request.rpe());
        log.info("SetLog {} updated by user {}", setId, user.getUsername());
        return WorkoutMapper.toSetLogDto(set);
    }

    @Transactional
    public void deleteSet(Long workoutLogId, Long exerciseLogId, Long setId, User user) {
        SetLog set = loadOwnedSet(workoutLogId, exerciseLogId, setId, user);
        setLogRepository.delete(set);
        log.info("SetLog {} deleted by user {}", setId, user.getUsername());
    }

    // Loads a set, confirms the user owns the workout log it belongs to, and asserts
    // the URL hierarchy. Mismatched / not-owned both surface as 404 (rule #2).
    private SetLog loadOwnedSet(Long workoutLogId, Long exerciseLogId, Long setId, User user) {
        SetLog set = setLogRepository.findById(setId)
                .orElseThrow(() -> ResourceNotFoundException.of("SetLog", setId));
        ExerciseLog exLog = set.getExerciseLog();
        if (!exLog.getWorkoutLog().getUser().getId().equals(user.getId())
                || !exLog.getId().equals(exerciseLogId)
                || !exLog.getWorkoutLog().getId().equals(workoutLogId)) {
            log.warn("User {} attempted to access SetLog {} via wrong path or without ownership",
                    user.getUsername(), setId);
            throw ResourceNotFoundException.of("SetLog", setId);
        }
        return set;
    }
}
