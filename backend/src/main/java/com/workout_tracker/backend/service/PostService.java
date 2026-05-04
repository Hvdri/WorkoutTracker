package com.workout_tracker.backend.service;

import com.workout_tracker.backend.dto.social.CreatePostRequest;
import com.workout_tracker.backend.dto.social.PostDto;
import com.workout_tracker.backend.exception.BusinessRuleViolationException;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.Post;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.WorkoutLog;
import com.workout_tracker.backend.model.enums.WorkoutStatus;
import com.workout_tracker.backend.repository.FollowRepository;
import com.workout_tracker.backend.repository.PostRepository;
import com.workout_tracker.backend.repository.WorkoutLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final WorkoutLogRepository workoutLogRepository;
    private final FollowRepository followRepository;
    private final UserService userService;

    // Business rule #3: only the owner of a WorkoutLog can post it.
    // Additional rules: the log must be COMPLETED, and a log can be posted at most once
    // (Post.workoutLog is unique). Both pre-checks live here so we get a clean 409 with
    // a domain message instead of a DataIntegrityViolation backstop.
    @Transactional
    public PostDto createPost(User user, CreatePostRequest request) {
        WorkoutLog workoutLog = workoutLogRepository.findByIdAndUser(request.workoutLogId(), user)
                .orElseThrow(() -> ResourceNotFoundException.of("WorkoutLog", request.workoutLogId()));

        if (workoutLog.getStatus() != WorkoutStatus.COMPLETED) {
            log.warn("User {} attempted to post WorkoutLog {} with status {}",
                    user.getUsername(), workoutLog.getId(), workoutLog.getStatus());
            throw new BusinessRuleViolationException(
                    "Only completed workouts can be shared as a post");
        }

        if (postRepository.existsByWorkoutLog(workoutLog)) {
            log.warn("User {} attempted to post duplicate Post for WorkoutLog {}",
                    user.getUsername(), workoutLog.getId());
            throw new BusinessRuleViolationException(
                    "WorkoutLog has already been shared as a post");
        }

        Post post = Post.builder()
                .user(user)
                .workoutLog(workoutLog)
                .caption(request.caption())
                .build();
        Post saved = postRepository.save(post);
        log.info("Post {} created by user {} for WorkoutLog {}",
                saved.getId(), user.getUsername(), workoutLog.getId());
        return SocialMapper.toPostDto(saved);
    }

    @Transactional
    public void deletePost(Long id, User user) {
        Post post = loadOwnedPost(id, user);
        postRepository.delete(post);
        log.info("Post {} deleted by user {}", id, user.getUsername());
    }

    @Transactional(readOnly = true)
    public Page<PostDto> getUserPosts(Long userId, Pageable pageable) {
        // Resolve the user first so an invalid id surfaces as 404 instead of an empty page.
        User author = userService.loadUser(userId);
        return postRepository.findByUserOrderByCreatedAtDesc(author, pageable)
                .map(SocialMapper::toPostDto);
    }

    // Feed = posts from everyone the user follows. If they follow nobody, return an
    // empty page early instead of issuing an `IN ()` query (some JPA providers reject it).
    @Transactional(readOnly = true)
    public Page<PostDto> getFeed(User user, Pageable pageable) {
        List<Long> followedIds = followRepository.findByFollower(user).stream()
                .map(f -> f.getFollowed().getId())
                .toList();
        if (followedIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return postRepository.findByUser_IdInOrderByCreatedAtDesc(followedIds, pageable)
                .map(SocialMapper::toPostDto);
    }

    // Loads a Post by id and verifies ownership. Like the workout-domain services,
    // miss-and-wrong-owner both surface as 404 to avoid leaking existence.
    private Post loadOwnedPost(Long id, User user) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Post", id));
        if (!post.getUser().getId().equals(user.getId())) {
            log.warn("User {} attempted to delete Post {} owned by user {}",
                    user.getUsername(), id, post.getUser().getUsername());
            throw ResourceNotFoundException.of("Post", id);
        }
        return post;
    }
}
