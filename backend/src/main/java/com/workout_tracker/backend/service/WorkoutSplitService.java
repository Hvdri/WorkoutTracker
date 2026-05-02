package com.workout_tracker.backend.service;

import com.workout_tracker.backend.dto.workout.CreateSplitRequest;
import com.workout_tracker.backend.dto.workout.WorkoutSplitDto;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.WorkoutSplit;
import com.workout_tracker.backend.repository.WorkoutSplitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkoutSplitService {

    private final WorkoutSplitRepository splitRepository;

    @Transactional
    public WorkoutSplitDto createSplit(User user, CreateSplitRequest request) {
        WorkoutSplit split = WorkoutSplit.builder()
                .name(request.name())
                .user(user)
                .active(false)
                .build();
        WorkoutSplit saved = splitRepository.save(split);
        log.info("WorkoutSplit {} created for user {}", saved.getId(), user.getUsername());
        return WorkoutMapper.toSplitDto(saved);
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutSplitDto> getActiveSplit(User user) {
        return splitRepository.findByUserAndActiveTrue(user).map(WorkoutMapper::toSplitDto);
    }

    @Transactional(readOnly = true)
    public List<WorkoutSplitDto> getAllSplits(User user) {
        return splitRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(WorkoutMapper::toSplitDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkoutSplitDto getSplitById(Long id, User user) {
        return WorkoutMapper.toSplitDto(loadOwnedSplit(id, user));
    }

    // Business rule #1: a user can have at most one active split. Activating a new one
    // implicitly deactivates all others for the same user, in a single transaction.
    @Transactional
    public WorkoutSplitDto activateSplit(Long id, User user) {
        WorkoutSplit target = loadOwnedSplit(id, user);

        splitRepository.findByUserAndActiveTrue(user).ifPresent(current -> {
            if (!current.getId().equals(target.getId())) {
                current.setActive(false);
            }
        });
        target.setActive(true);

        log.info("WorkoutSplit {} activated for user {}", id, user.getUsername());
        return WorkoutMapper.toSplitDto(target);
    }

    @Transactional
    public void deleteSplit(Long id, User user) {
        WorkoutSplit split = loadOwnedSplit(id, user);
        splitRepository.delete(split);
        log.info("WorkoutSplit {} deleted by user {}", id, user.getUsername());
    }

    // Loads a split by id and verifies ownership in one place. Returns the entity
    // (rather than DTO) so callers can mutate it; mappers handle DTO conversion.
    // Both miss-and-wrong-owner cases surface as 404 to avoid leaking existence
    // (ProjectContext rule #2).
    WorkoutSplit loadOwnedSplit(Long id, User user) {
        WorkoutSplit split = splitRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("WorkoutSplit", id));
        if (!split.getUser().getId().equals(user.getId())) {
            log.warn("User {} attempted to access WorkoutSplit {} owned by user {}",
                    user.getUsername(), id, split.getUser().getUsername());
            throw ResourceNotFoundException.of("WorkoutSplit", id);
        }
        return split;
    }
}
