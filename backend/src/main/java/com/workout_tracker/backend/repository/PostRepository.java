package com.workout_tracker.backend.repository;

import com.workout_tracker.backend.model.Post;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.WorkoutLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByUser_IdInOrderByCreatedAtDesc(List<Long> userIds, Pageable pageable);

    Page<Post> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Optional<Post> findByWorkoutLog(WorkoutLog workoutLog);

    boolean existsByWorkoutLog(WorkoutLog workoutLog);
}
