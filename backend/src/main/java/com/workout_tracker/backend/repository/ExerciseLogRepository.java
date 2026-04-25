package com.workout_tracker.backend.repository;

import com.workout_tracker.backend.model.ExerciseLog;
import com.workout_tracker.backend.model.WorkoutLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseLogRepository extends JpaRepository<ExerciseLog, Long> {

    List<ExerciseLog> findByWorkoutLog(WorkoutLog workoutLog);
}
