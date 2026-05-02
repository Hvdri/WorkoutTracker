package com.workout_tracker.backend.service;

import com.workout_tracker.backend.dto.workout.CreateSplitRequest;
import com.workout_tracker.backend.dto.workout.WorkoutSplitDto;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.WorkoutSplit;
import com.workout_tracker.backend.repository.WorkoutSplitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutSplitServiceTest {

    @Mock WorkoutSplitRepository splitRepository;
    @InjectMocks WorkoutSplitService splitService;

    @Test
    void createSplit_savesAsInactive() {
        User user = userWithId(1L);
        when(splitRepository.save(any(WorkoutSplit.class))).thenAnswer(inv -> {
            WorkoutSplit s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        WorkoutSplitDto dto = splitService.createSplit(user, new CreateSplitRequest("PPL"));

        ArgumentCaptor<WorkoutSplit> captor = ArgumentCaptor.forClass(WorkoutSplit.class);
        verify(splitRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
        assertThat(captor.getValue().getName()).isEqualTo("PPL");
        assertThat(dto.isActive()).isFalse();
    }

    @Test
    void activateSplit_deactivatesCurrentlyActive() {
        // Business rule #1: activating a new split deactivates the previous active one.
        User user = userWithId(1L);
        WorkoutSplit current = splitWithId(5L, user, true);
        WorkoutSplit target = splitWithId(7L, user, false);

        when(splitRepository.findById(7L)).thenReturn(Optional.of(target));
        when(splitRepository.findByUserAndActiveTrue(user)).thenReturn(Optional.of(current));

        WorkoutSplitDto dto = splitService.activateSplit(7L, user);

        assertThat(current.isActive()).isFalse();
        assertThat(target.isActive()).isTrue();
        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.isActive()).isTrue();
    }

    @Test
    void activateSplit_alreadyActiveSameId_isNoop() {
        // If the user re-activates the already-active split, leave it active and don't toggle.
        User user = userWithId(1L);
        WorkoutSplit target = splitWithId(7L, user, true);

        when(splitRepository.findById(7L)).thenReturn(Optional.of(target));
        when(splitRepository.findByUserAndActiveTrue(user)).thenReturn(Optional.of(target));

        WorkoutSplitDto dto = splitService.activateSplit(7L, user);

        assertThat(target.isActive()).isTrue();
        assertThat(dto.isActive()).isTrue();
    }

    @Test
    void activateSplit_wrongOwner_throwsResourceNotFound() {
        // Standardized on 404 to avoid leaking existence to non-owners (rule #2).
        User attacker = userWithId(2L);
        WorkoutSplit target = splitWithId(7L, userWithId(1L), false);
        when(splitRepository.findById(7L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> splitService.activateSplit(7L, attacker))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSplitById_unknown_throwsResourceNotFound() {
        when(splitRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> splitService.getSplitById(404L, userWithId(1L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSplitById_wrongOwner_throwsResourceNotFound() {
        User attacker = userWithId(2L);
        WorkoutSplit target = splitWithId(7L, userWithId(1L), false);
        when(splitRepository.findById(7L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> splitService.getSplitById(7L, attacker))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static User userWithId(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        return u;
    }

    private static WorkoutSplit splitWithId(Long id, User user, boolean active) {
        return WorkoutSplit.builder().id(id).name("split-" + id).user(user).active(active).build();
    }
}
