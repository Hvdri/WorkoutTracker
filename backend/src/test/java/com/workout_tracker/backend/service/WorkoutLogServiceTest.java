package com.workout_tracker.backend.service;

import com.workout_tracker.backend.client.NotificationClient;
import com.workout_tracker.backend.dto.workout.CreateWorkoutLogRequest;
import com.workout_tracker.backend.dto.workout.UpdateWorkoutLogRequest;
import com.workout_tracker.backend.dto.workout.WorkoutLogDto;
import com.workout_tracker.backend.exception.BusinessRuleViolationException;
import com.workout_tracker.backend.exception.ResourceAccessDeniedException;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.WorkoutLog;
import com.workout_tracker.backend.model.WorkoutSplit;
import com.workout_tracker.backend.model.WorkoutTemplate;
import com.workout_tracker.backend.model.enums.WorkoutStatus;
import com.workout_tracker.backend.repository.WorkoutLogRepository;
import com.workout_tracker.backend.repository.WorkoutSplitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutLogServiceTest {

    @Mock WorkoutLogRepository workoutLogRepository;
    @Mock WorkoutSplitRepository splitRepository;
    @Mock WorkoutTemplateService templateService;
    @Mock NotificationClient notificationClient;

    @InjectMocks WorkoutLogService workoutLogService;

    // ── createLog ────────────────────────────────────────────────────────────

    @Test
    void createLog_noActiveSplit_throwsBusinessRuleViolation() {
        User user = userWithId(1L);
        when(splitRepository.findByUserAndActiveTrue(user)).thenReturn(Optional.empty());

        CreateWorkoutLogRequest req = new CreateWorkoutLogRequest(
                LocalDate.now(), 99L, null, null);
        assertThatThrownBy(() -> workoutLogService.createLog(user, req))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("active");

        verify(workoutLogRepository, never()).save(any());
    }

    @Test
    void createLog_templateBelongsToOtherSplit_throwsBusinessRuleViolation() {
        // Business rule #2: template's split must be the user's active split.
        User user = userWithId(1L);
        WorkoutSplit activeSplit = splitWithId(10L, user, true);
        WorkoutSplit otherSplit = splitWithId(20L, user, false);
        WorkoutTemplate templateInOther = templateWithId(99L, otherSplit);

        when(splitRepository.findByUserAndActiveTrue(user)).thenReturn(Optional.of(activeSplit));
        when(templateService.loadOwnedTemplate(99L, user)).thenReturn(templateInOther);

        CreateWorkoutLogRequest req = new CreateWorkoutLogRequest(
                LocalDate.now(), 99L, null, null);

        assertThatThrownBy(() -> workoutLogService.createLog(user, req))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("active");

        verify(workoutLogRepository, never()).save(any());
    }

    @Test
    void createLog_templateInActiveSplit_savesAsInProgress() {
        User user = userWithId(1L);
        WorkoutSplit activeSplit = splitWithId(10L, user, true);
        WorkoutTemplate template = templateWithId(99L, activeSplit);

        when(splitRepository.findByUserAndActiveTrue(user)).thenReturn(Optional.of(activeSplit));
        when(templateService.loadOwnedTemplate(99L, user)).thenReturn(template);
        when(workoutLogRepository.save(any(WorkoutLog.class))).thenAnswer(inv -> {
            WorkoutLog wl = inv.getArgument(0);
            wl.setId(500L);
            return wl;
        });

        CreateWorkoutLogRequest req = new CreateWorkoutLogRequest(
                LocalDate.of(2026, 1, 15), 99L, "https://example.com/p.jpg", "felt strong");
        WorkoutLogDto dto = workoutLogService.createLog(user, req);

        assertThat(dto.id()).isEqualTo(500L);
        assertThat(dto.status()).isEqualTo(WorkoutStatus.IN_PROGRESS.name());
        assertThat(dto.templateId()).isEqualTo(99L);
    }

    // ── ownership: getLogById / completeLog ──────────────────────────────────

    @Test
    void getLogById_wrongOwner_throwsResourceNotFound() {
        // findByIdAndUser returns empty for non-owners; we surface that as 404 (not 403)
        // to avoid revealing whether the resource exists.
        User attacker = userWithId(2L);
        when(workoutLogRepository.findByIdAndUser(50L, attacker)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutLogService.getLogById(50L, attacker))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void completeLog_setsStatusToCompleted() {
        User owner = userWithId(1L);
        WorkoutLog log = logWithId(50L, owner, templateWithId(99L, splitWithId(10L, owner, true)));
        when(workoutLogRepository.findByIdAndUser(50L, owner)).thenReturn(Optional.of(log));

        WorkoutLogDto dto = workoutLogService.completeLog(50L, owner);

        assertThat(log.getStatus()).isEqualTo(WorkoutStatus.COMPLETED);
        assertThat(dto.status()).isEqualTo(WorkoutStatus.COMPLETED.name());
    }

    @Test
    void completeLog_wrongOwner_throwsResourceNotFound() {
        User attacker = userWithId(2L);
        when(workoutLogRepository.findByIdAndUser(50L, attacker)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutLogService.completeLog(50L, attacker))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateLog patch semantics ────────────────────────────────────────────

    @Test
    void updateLog_nullFieldsAreIgnored() {
        User owner = userWithId(1L);
        WorkoutLog log = logWithId(50L, owner, templateWithId(99L, splitWithId(10L, owner, true)));
        log.setNotes("original notes");
        log.setPhotoUrl("original.jpg");

        when(workoutLogRepository.findByIdAndUser(50L, owner)).thenReturn(Optional.of(log));

        UpdateWorkoutLogRequest req = new UpdateWorkoutLogRequest(null, "new notes", null);
        workoutLogService.updateLog(50L, owner, req);

        // Only notes should change — photoUrl and status untouched.
        assertThat(log.getNotes()).isEqualTo("new notes");
        assertThat(log.getPhotoUrl()).isEqualTo("original.jpg");
        assertThat(log.getStatus()).isEqualTo(WorkoutStatus.IN_PROGRESS);
    }

    // ── ResourceAccessDeniedException reaching the controller ────────────────
    // Sanity: WorkoutLogService doesn't throw 403 itself (uses 404 via findByIdAndUser),
    // but the type compiles — keep the import live.
    @SuppressWarnings("unused")
    private static final Class<?> ACCESS_DENIED = ResourceAccessDeniedException.class;

    // ── helpers ──────────────────────────────────────────────────────────────

    private static User userWithId(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        return u;
    }

    private static WorkoutSplit splitWithId(Long id, User user, boolean active) {
        return WorkoutSplit.builder().id(id).name("split-" + id).user(user).active(active).build();
    }

    private static WorkoutTemplate templateWithId(Long id, WorkoutSplit split) {
        return WorkoutTemplate.builder().id(id).name("template-" + id).split(split).build();
    }

    private static WorkoutLog logWithId(Long id, User user, WorkoutTemplate template) {
        return WorkoutLog.builder()
                .id(id)
                .user(user)
                .template(template)
                .date(LocalDate.now())
                .status(WorkoutStatus.IN_PROGRESS)
                .build();
    }
}
