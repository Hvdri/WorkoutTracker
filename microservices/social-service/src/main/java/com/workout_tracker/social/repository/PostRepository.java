package com.workout_tracker.social.repository;

import com.workout_tracker.social.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByUserIdInOrderByCreatedAtDescIdDesc(List<Long> userIds, Pageable pageable);

    Page<Post> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    Page<Post> findByUserIdNotInOrderByCreatedAtDescIdDesc(Collection<Long> excludedUserIds, Pageable pageable);

    boolean existsByWorkoutLogId(Long workoutLogId);
}
