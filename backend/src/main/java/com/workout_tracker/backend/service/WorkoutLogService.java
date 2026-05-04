package com.workout_tracker.backend.service;

import com.workout_tracker.backend.dto.workout.CreateWorkoutLogRequest;
import com.workout_tracker.backend.dto.workout.UpdateWorkoutLogRequest;
import com.workout_tracker.backend.dto.workout.WorkoutLogDto;
import com.workout_tracker.backend.exception.BusinessRuleViolationException;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.WorkoutLog;
import com.workout_tracker.backend.model.WorkoutSplit;
import com.workout_tracker.backend.model.WorkoutTemplate;
import com.workout_tracker.backend.model.enums.WorkoutStatus;
import com.workout_tracker.backend.repository.WorkoutLogRepository;
import com.workout_tracker.backend.repository.WorkoutSplitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkoutLogService {

    private final WorkoutLogRepository workoutLogRepository;
    private final WorkoutSplitRepository splitRepository;
    private final WorkoutTemplateService templateService;

    // Business rule #2: a WorkoutLog must reference a WorkoutTemplate that belongs to
    // the user's currently active WorkoutSplit. Without an active split, no logging is allowed.
    @Transactional
    public WorkoutLogDto createLog(User user, CreateWorkoutLogRequest request) {
        WorkoutSplit activeSplit = splitRepository.findByUserAndActiveTrue(user)
                .orElseThrow(() -> {
                    log.warn("User {} attempted to log a workout with no active split", user.getUsername());
                    return new BusinessRuleViolationException(
                            "No active WorkoutSplit — activate one before logging a workout");
                });

        WorkoutTemplate template = templateService.loadOwnedTemplate(request.templateId(), user);
        if (!template.getSplit().getId().equals(activeSplit.getId())) {
            log.warn("User {} tried to log against template {} from a non-active split",
                    user.getUsername(), template.getId());
            throw new BusinessRuleViolationException(
                    "WorkoutTemplate does not belong to the currently active WorkoutSplit");
        }

        WorkoutLog log_ = WorkoutLog.builder()
                .user(user)
                .template(template)
                .date(request.date())
                .photoUrl(request.photoUrl())
                .notes(request.notes())
                .status(WorkoutStatus.IN_PROGRESS)
                .build();

        WorkoutLog saved = workoutLogRepository.save(log_);
        log.info("WorkoutLog {} created for user {} (template {}, date {})",
                saved.getId(), user.getUsername(), template.getId(), saved.getDate());
        return WorkoutMapper.toWorkoutLogDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutLogDto> getLogHistory(User user, Pageable pageable) {
        return workoutLogRepository.findByUserOrderByDateDesc(user, pageable)
                .map(WorkoutMapper::toWorkoutLogDto);
    }

    @Transactional(readOnly = true)
    public WorkoutLogDto getLogById(Long id, User user) {
        return WorkoutMapper.toWorkoutLogDto(loadOwnedLog(id, user));
    }

    @Transactional
    public WorkoutLogDto updateLog(Long id, User user, UpdateWorkoutLogRequest request) {
        WorkoutLog log_ = loadOwnedLog(id, user);
        if (request.photoUrl() != null) log_.setPhotoUrl(request.photoUrl());
        if (request.notes() != null) log_.setNotes(request.notes());
        if (request.status() != null) log_.setStatus(request.status());
        // Dirty-checked on transaction commit — no explicit save() needed.
        log.info("WorkoutLog {} updated by user {}", id, user.getUsername());
        return WorkoutMapper.toWorkoutLogDto(log_);
    }

    @Transactional
    public void deleteLog(Long id, User user) {
        WorkoutLog log_ = loadOwnedLog(id, user);
        workoutLogRepository.delete(log_);
        log.info("WorkoutLog {} deleted by user {}", id, user.getUsername());
    }

    @Transactional
    public WorkoutLogDto completeLog(Long id, User user) {
        WorkoutLog log_ = loadOwnedLog(id, user);
        log_.setStatus(WorkoutStatus.COMPLETED);
        log.info("WorkoutLog {} completed by user {}", id, user.getUsername());
        return WorkoutMapper.toWorkoutLogDto(log_);
    }

    // Loads a workout log by id and verifies ownership. The repo's findByIdAndUser already
    // does this in one query — we use it instead of fetching then comparing user ids.
    // Package-private so child-resource services (ExerciseLog, SetLog) can reuse it.
    WorkoutLog loadOwnedLog(Long id, User user) {
        return workoutLogRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> ResourceNotFoundException.of("WorkoutLog", id));
    }
}
