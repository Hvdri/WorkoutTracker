package com.workout_tracker.backend.service;

import com.workout_tracker.backend.dto.workout.CreateExerciseRequest;
import com.workout_tracker.backend.dto.workout.ExerciseDto;
import com.workout_tracker.backend.exception.BusinessRuleViolationException;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.Exercise;
import com.workout_tracker.backend.model.enums.MuscleGroup;
import com.workout_tracker.backend.repository.ExerciseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock ExerciseRepository exerciseRepository;

    @InjectMocks ExerciseService exerciseService;

    // ── findExercises (filter combinations) ─────────────────────────────────

    @Test
    void findExercises_noFilters_callsFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        when(exerciseRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        exerciseService.findExercises(null, null, pageable);

        verify(exerciseRepository).findAll(pageable);
        verify(exerciseRepository, never()).findByMuscleGroup(any(), any());
        verify(exerciseRepository, never()).findByNameContainingIgnoreCase(any(), any());
    }

    @Test
    void findExercises_muscleGroupOnly_callsMuscleGroupQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        when(exerciseRepository.findByMuscleGroup(MuscleGroup.CHEST, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        exerciseService.findExercises(MuscleGroup.CHEST, null, pageable);

        verify(exerciseRepository).findByMuscleGroup(MuscleGroup.CHEST, pageable);
    }

    @Test
    void findExercises_nameOnly_callsNameQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        when(exerciseRepository.findByNameContainingIgnoreCase("press", pageable))
                .thenReturn(new PageImpl<>(List.of()));

        exerciseService.findExercises(null, "press", pageable);

        verify(exerciseRepository).findByNameContainingIgnoreCase("press", pageable);
    }

    @Test
    void findExercises_blankName_treatedAsNoFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        when(exerciseRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        exerciseService.findExercises(null, "   ", pageable);

        verify(exerciseRepository).findAll(pageable);
        verify(exerciseRepository, never()).findByNameContainingIgnoreCase(any(), any());
    }

    @Test
    void findExercises_bothFilters_callsCombinedQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        when(exerciseRepository.findByMuscleGroupAndNameContainingIgnoreCase(
                MuscleGroup.CHEST, "press", pageable))
                .thenReturn(new PageImpl<>(List.of()));

        exerciseService.findExercises(MuscleGroup.CHEST, "press", pageable);

        verify(exerciseRepository).findByMuscleGroupAndNameContainingIgnoreCase(
                MuscleGroup.CHEST, "press", pageable);
    }

    // ── getExerciseById ─────────────────────────────────────────────────────

    @Test
    void getExerciseById_existingId_returnsDto() {
        Exercise exercise = exerciseEntity(7L, "Squat", MuscleGroup.LEGS);
        when(exerciseRepository.findById(7L)).thenReturn(Optional.of(exercise));

        ExerciseDto dto = exerciseService.getExerciseById(7L);

        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.name()).isEqualTo("Squat");
        assertThat(dto.muscleGroup()).isEqualTo("LEGS");
    }

    @Test
    void getExerciseById_unknownId_throwsResourceNotFound() {
        when(exerciseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exerciseService.getExerciseById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
    }

    // ── createExercise ──────────────────────────────────────────────────────

    @Test
    void createExercise_uniqueName_savesAndReturnsDto() {
        CreateExerciseRequest req = new CreateExerciseRequest("Front Squat", "desc", MuscleGroup.LEGS, null);
        when(exerciseRepository.existsByNameIgnoreCase("Front Squat")).thenReturn(false);
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> {
            Exercise e = inv.getArgument(0);
            e.setId(42L);
            return e;
        });

        ExerciseDto dto = exerciseService.createExercise(req);

        ArgumentCaptor<Exercise> captor = ArgumentCaptor.forClass(Exercise.class);
        verify(exerciseRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Front Squat");
        assertThat(captor.getValue().getMuscleGroup()).isEqualTo(MuscleGroup.LEGS);
        assertThat(dto.id()).isEqualTo(42L);
    }

    @Test
    void createExercise_duplicateName_throwsBusinessRuleViolation() {
        CreateExerciseRequest req = new CreateExerciseRequest("Bench Press", null, MuscleGroup.CHEST, null);
        when(exerciseRepository.existsByNameIgnoreCase("Bench Press")).thenReturn(true);

        assertThatThrownBy(() -> exerciseService.createExercise(req))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already exists");

        verify(exerciseRepository, never()).save(any());
    }

    // ── updateExercise ──────────────────────────────────────────────────────

    @Test
    void updateExercise_unknownId_throwsResourceNotFound() {
        when(exerciseRepository.findById(404L)).thenReturn(Optional.empty());

        CreateExerciseRequest req = new CreateExerciseRequest("X", null, MuscleGroup.CORE, null);
        assertThatThrownBy(() -> exerciseService.updateExercise(404L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateExercise_renameToTakenName_throwsBusinessRuleViolation() {
        Exercise existing = exerciseEntity(1L, "Cable Pullover", MuscleGroup.BACK);
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(existing));
        // Caller is renaming to a name that's already taken by a *different* row.
        when(exerciseRepository.existsByNameIgnoreCase("Bench Press")).thenReturn(true);

        CreateExerciseRequest req = new CreateExerciseRequest("Bench Press", null, MuscleGroup.CHEST, null);
        assertThatThrownBy(() -> exerciseService.updateExercise(1L, req))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void updateExercise_sameNameDifferentDescription_doesNotCheckUniqueness() {
        Exercise existing = exerciseEntity(1L, "Cable Pullover", MuscleGroup.BACK);
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(existing));

        CreateExerciseRequest req = new CreateExerciseRequest(
                "Cable Pullover", "Updated description", MuscleGroup.BACK, null);

        ExerciseDto dto = exerciseService.updateExercise(1L, req);

        // Same name (case-insensitive) → no need to query existsByNameIgnoreCase
        verify(exerciseRepository, never()).existsByNameIgnoreCase(anyString());
        assertThat(dto.description()).isEqualTo("Updated description");
        assertThat(existing.getDescription()).isEqualTo("Updated description");
    }

    // ── deleteExercise ──────────────────────────────────────────────────────

    @Test
    void deleteExercise_existing_callsDelete() {
        when(exerciseRepository.existsById(1L)).thenReturn(true);

        exerciseService.deleteExercise(1L);

        verify(exerciseRepository).deleteById(1L);
    }

    @Test
    void deleteExercise_unknown_throwsResourceNotFound() {
        when(exerciseRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> exerciseService.deleteExercise(404L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(exerciseRepository, never()).deleteById(eq(404L));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static Exercise exerciseEntity(Long id, String name, MuscleGroup group) {
        return Exercise.builder().id(id).name(name).muscleGroup(group).build();
    }
}
