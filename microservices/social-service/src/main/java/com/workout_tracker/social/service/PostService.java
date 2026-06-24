package com.workout_tracker.social.service;

import com.workout_tracker.social.client.LogSummary;
import com.workout_tracker.social.client.MainAppClient;
import com.workout_tracker.social.client.NotificationClient;
import com.workout_tracker.social.dto.CreatePostRequest;
import com.workout_tracker.social.dto.PostDto;
import com.workout_tracker.social.dto.UserSummaryDto;
import com.workout_tracker.social.exception.BusinessRuleViolationException;
import com.workout_tracker.social.exception.ResourceNotFoundException;
import com.workout_tracker.social.exception.UpstreamUnavailableException;
import com.workout_tracker.social.model.Post;
import com.workout_tracker.social.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final MainAppClient mainAppClient;
    private final NotificationClient notificationClient;
    private final SocialService socialService;

    // createPost CANNOT proceed on degraded MainAppClient data — we must know the log
    // status and owner authoritatively. If the breaker is open, surface 503 and let
    // the user retry.
    @Transactional
    public PostDto createPost(Long currentUserId, String currentUsername, CreatePostRequest request) {
        LogSummary logSummary = mainAppClient.getLogSummary(request.workoutLogId());
        if (logSummary == null) {
            throw ResourceNotFoundException.of("WorkoutLog", request.workoutLogId());
        }
        if (logSummary.isDegraded()) {
            throw new UpstreamUnavailableException(
                    "Cannot validate workout log right now — please retry shortly");
        }
        if (!currentUserId.equals(logSummary.ownerId())) {
            log.warn("User {} attempted to post WorkoutLog {} owned by {}",
                    currentUserId, request.workoutLogId(), logSummary.ownerId());
            // Match the monolith's "404 on wrong owner" posture — don't leak which log
            // ids exist. The user sees "log not found" either way.
            throw ResourceNotFoundException.of("WorkoutLog", request.workoutLogId());
        }
        if (!"COMPLETED".equals(logSummary.status())) {
            log.warn("User {} attempted to post WorkoutLog {} with status {}",
                    currentUserId, request.workoutLogId(), logSummary.status());
            throw new BusinessRuleViolationException(
                    "Only completed workouts can be shared as a post");
        }
        if (postRepository.existsByWorkoutLogId(request.workoutLogId())) {
            log.warn("Duplicate Post creation attempt for WorkoutLog {} by user {}",
                    request.workoutLogId(), currentUserId);
            throw new BusinessRuleViolationException(
                    "WorkoutLog has already been shared as a post");
        }

        Post post = Post.builder()
                .userId(currentUserId)
                .workoutLogId(request.workoutLogId())
                .caption(request.caption())
                .build();
        Post saved = postRepository.save(post);
        log.info("Post {} created by user {} for WorkoutLog {}",
                saved.getId(), currentUserId, request.workoutLogId());

        // Fan out POST_CREATED notifications to every follower of the author.
        // Each call is wrapped in the notification-service circuit breaker — if any
        // single fire blows up the breaker, the remaining followers in this round
        // get fallbacked (logged). The post itself is already saved at this point,
        // so a notification storm can't undo the create. For our scale (small
        // demos) this loop is fine; production would batch via a single POST or a
        // message broker (Kafka) per the prof's lab7_start pattern.
        List<Long> followerIds = socialService.getFollowerIds(currentUserId);
        for (Long followerId : followerIds) {
            notificationClient.notifyPostCreated(followerId, currentUsername, saved.getWorkoutLogId());
        }

        // Enrich for the response. Author lookup is best-effort — we just created the
        // post, the user definitely exists. If the breaker is open, "Unknown" leaks
        // briefly until refetch.
        UserSummaryDto author = mainAppClient.getUser(currentUserId);
        return SocialMapper.toPostDto(saved, author, logSummary);
    }

    @Transactional
    public void deletePost(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> ResourceNotFoundException.of("Post", postId));
        if (!post.getUserId().equals(currentUserId)) {
            log.warn("User {} attempted to delete Post {} owned by {}",
                    currentUserId, postId, post.getUserId());
            // 404 instead of 403 — same posture as the monolith's loadOwnedPost.
            throw ResourceNotFoundException.of("Post", postId);
        }
        postRepository.delete(post);
        log.info("Post {} deleted by user {}", postId, currentUserId);
    }

    // Posts by a specific user. The monolith pre-validates that the user exists; we do
    // the same via MainAppClient.getUser. A null author means 404 here.
    @Transactional(readOnly = true)
    public Page<PostDto> getUserPosts(Long userId, Pageable pageable) {
        UserSummaryDto author = mainAppClient.getUser(userId);
        if (author == null) {
            throw ResourceNotFoundException.of("User", userId);
        }
        return postRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable)
                .map(post -> SocialMapper.toPostDto(
                        post, author, mainAppClient.getLogSummary(post.getWorkoutLogId())));
    }

    @Transactional(readOnly = true)
    public Page<PostDto> getFeed(Long currentUserId, Pageable pageable) {
        List<Long> followedIds = socialService.getFollowedUserIds(currentUserId);
        if (followedIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return postRepository.findByUserIdInOrderByCreatedAtDescIdDesc(followedIds, pageable)
                .map(this::enrich);
    }

    @Transactional(readOnly = true)
    public Page<PostDto> getDiscovery(Long currentUserId, Pageable pageable) {
        Set<Long> excluded = new HashSet<>(socialService.getFollowedUserIds(currentUserId));
        excluded.add(currentUserId);
        return postRepository.findByUserIdNotInOrderByCreatedAtDescIdDesc(excluded, pageable)
                .map(this::enrich);
    }

    private PostDto enrich(Post post) {
        return SocialMapper.toPostDto(
            post,
            mainAppClient.getUser(post.getUserId()),
            mainAppClient.getLogSummary(post.getWorkoutLogId())
        );
    }
}
