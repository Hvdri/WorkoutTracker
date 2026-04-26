package com.workout_tracker.backend.repository;

import com.workout_tracker.backend.model.Exercise;
import com.workout_tracker.backend.model.enums.MuscleGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    Page<Exercise> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Exercise> findByMuscleGroup(MuscleGroup muscleGroup, Pageable pageable);

    Page<Exercise> findByMuscleGroupAndNameContainingIgnoreCase(MuscleGroup muscleGroup, String name, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);
}
