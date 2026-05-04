package com.workout_tracker.backend.repository;

import com.workout_tracker.backend.model.WorkoutSplit;
import com.workout_tracker.backend.model.WorkoutTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplate, Long> {

    List<WorkoutTemplate> findBySplitOrderByOrderIndexAsc(WorkoutSplit split);
}
