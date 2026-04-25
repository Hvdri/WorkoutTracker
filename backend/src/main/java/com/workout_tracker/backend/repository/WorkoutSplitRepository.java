package com.workout_tracker.backend.repository;

import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.WorkoutSplit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkoutSplitRepository extends JpaRepository<WorkoutSplit, Long> {

    Optional<WorkoutSplit> findByUserAndActiveTrue(User user);

    List<WorkoutSplit> findByUserOrderByCreatedAtDesc(User user);
}
