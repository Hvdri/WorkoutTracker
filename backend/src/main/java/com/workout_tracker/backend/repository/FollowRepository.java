package com.workout_tracker.backend.repository;

import com.workout_tracker.backend.model.Follow;
import com.workout_tracker.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerAndFollowed(User follower, User followed);

    List<Follow> findByFollower(User follower);

    List<Follow> findByFollowed(User followed);

    Optional<Follow> findByFollowerAndFollowed(User follower, User followed);
}
