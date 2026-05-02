package com.workout_tracker.backend.service;

import com.workout_tracker.backend.dto.workout.CreateSetLogRequest;
import com.workout_tracker.backend.dto.workout.SetLogDto;
import com.workout_tracker.backend.dto.workout.UpdateSetLogRequest;
import com.workout_tracker.backend.exception.BusinessRuleViolationException;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.ExerciseLog;
import com.workout_tracker.backend.model.SetLog;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.WorkoutLog;
import com.workout_tracker.backend.repository.SetLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Constants used across tests so the path-chain assertions in the service have
// matching ids. The "owner" workout log id is 1L; exercise log under it is 7L.
@ExtendWith(MockitoExtension.class)
class SetLogServiceTest {

    private static final long WORKOUT_LOG_ID = 1L;
    private static final long EXERCISE_LOG_ID = 7L;

    @Mock SetLogRepository setLogRepository;
    @Mock ExerciseLogService exerciseLogService;

    @InjectMocks SetLogService setLogService;

    @Test
    void addSet_persistsWithParentExerciseLog() {
        User owner = userWithId(1L);
        ExerciseLog parent = exerciseLogWithOwner(owner, EXERCISE_LOG_ID);

        when(exerciseLogService.loadOwnedExerciseLog(WORKOUT_LOG_ID, EXERCISE_LOG_ID, owner))
                .thenReturn(parent);
        when(setLogRepository.save(any(SetLog.class))).thenAnswer(inv -> {
            SetLog s = inv.getArgument(0);
            s.setId(99L);
            return s;
        });

        CreateSetLogRequest req = new CreateSetLogRequest(1, new BigDecimal("80.0"), 8, 8);
        SetLogDto dto = setLogService.addSet(WORKOUT_LOG_ID, EXERCISE_LOG_ID, owner, req);

        assertThat(dto.id()).isEqualTo(99L);
        assertThat(dto.weightKg()).isEqualByComparingTo("80.0");
    }

    @Test
    void addSet_duplicateSetNumber_throwsBusinessRuleViolation() {
        User owner = userWithId(1L);
        ExerciseLog parent = exerciseLogWithOwner(owner, EXERCISE_LOG_ID);
        // parent already contains setNumber=1
        SetLog existing = SetLog.builder()
                .id(50L).exerciseLog(parent).setNumber(1)
                .weightKg(new BigDecimal("60.0")).reps(10).build();
        parent.getSets().add(existing);

        when(exerciseLogService.loadOwnedExerciseLog(WORKOUT_LOG_ID, EXERCISE_LOG_ID, owner))
                .thenReturn(parent);

        CreateSetLogRequest req = new CreateSetLogRequest(1, new BigDecimal("80.0"), 8, 8);
        assertThatThrownBy(() -> setLogService.addSet(WORKOUT_LOG_ID, EXERCISE_LOG_ID, owner, req))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Set number 1");

        verify(setLogRepository, never()).save(any());
    }

    @Test
    void updateSet_nullFieldsIgnored() {
        User owner = userWithId(1L);
        SetLog set = setWithId(50L, owner, new BigDecimal("60.0"), 10, 7);
        when(setLogRepository.findById(50L)).thenReturn(Optional.of(set));

        // Only update reps; weight and rpe stay.
        setLogService.updateSet(WORKOUT_LOG_ID, EXERCISE_LOG_ID, 50L, owner,
                new UpdateSetLogRequest(null, 12, null));

        assertThat(set.getReps()).isEqualTo(12);
        assertThat(set.getWeightKg()).isEqualByComparingTo("60.0");
        assertThat(set.getRpe()).isEqualTo(7);
    }

    @Test
    void updateSet_wrongOwner_throwsResourceNotFound() {
        // 403 was leaking existence; standardized on 404 (ProjectContext rule #2).
        User attacker = userWithId(2L);
        SetLog set = setWithId(50L, userWithId(1L), new BigDecimal("60.0"), 10, 7);
        when(setLogRepository.findById(50L)).thenReturn(Optional.of(set));

        assertThatThrownBy(() -> setLogService.updateSet(WORKOUT_LOG_ID, EXERCISE_LOG_ID, 50L,
                attacker, new UpdateSetLogRequest(null, 12, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateSet_pathChainMismatch_throwsResourceNotFound() {
        // Set exists and is owned, but the URL was /api/logs/{wrong}/exercises/{wrong}/sets/{realId}.
        User owner = userWithId(1L);
        SetLog set = setWithId(50L, owner, new BigDecimal("60.0"), 10, 7);
        when(setLogRepository.findById(50L)).thenReturn(Optional.of(set));

        assertThatThrownBy(() -> setLogService.updateSet(99_999L, EXERCISE_LOG_ID, 50L, owner,
                new UpdateSetLogRequest(null, 12, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteSet_unknown_throwsResourceNotFound() {
        when(setLogRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> setLogService.deleteSet(WORKOUT_LOG_ID, EXERCISE_LOG_ID, 404L,
                userWithId(1L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(setLogRepository, never()).delete(any());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static User userWithId(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        return u;
    }

    private static ExerciseLog exerciseLogWithOwner(User owner, Long id) {
        WorkoutLog wl = WorkoutLog.builder().id(WORKOUT_LOG_ID).user(owner).build();
        ExerciseLog ex = ExerciseLog.builder().id(id).workoutLog(wl).build();
        ex.setSets(new ArrayList<>());
        return ex;
    }

    private static SetLog setWithId(Long id, User owner, BigDecimal weight, int reps, Integer rpe) {
        ExerciseLog parent = exerciseLogWithOwner(owner, EXERCISE_LOG_ID);
        return SetLog.builder()
                .id(id)
                .exerciseLog(parent)
                .setNumber(1)
                .weightKg(weight)
                .reps(reps)
                .rpe(rpe)
                .build();
    }
}
