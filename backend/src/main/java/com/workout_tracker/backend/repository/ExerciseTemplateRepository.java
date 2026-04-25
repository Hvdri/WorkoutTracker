package com.workout_tracker.backend.repository;

import com.workout_tracker.backend.model.ExerciseTemplate;
import com.workout_tracker.backend.model.WorkoutTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseTemplateRepository extends JpaRepository<ExerciseTemplate, Long> {

    List<ExerciseTemplate> findByTemplateOrderByOrderIndexAsc(WorkoutTemplate template);
}
