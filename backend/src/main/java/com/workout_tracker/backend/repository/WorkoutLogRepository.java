package com.workout_tracker.backend.repository;

import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.WorkoutLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, Long> {

    // Built-in OrderByDateDesc is the default sort when the caller's Pageable is unsorted.
    // A Pageable with its own sort (e.g. ?sort=date,asc) overrides this.
    Page<WorkoutLog> findByUserOrderByDateDesc(User user, Pageable pageable);

    Optional<WorkoutLog> findByIdAndUser(Long id, User user);
}
