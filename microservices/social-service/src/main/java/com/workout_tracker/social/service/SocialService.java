package com.workout_tracker.social.service;

import com.workout_tracker.social.client.MainAppClient;
import com.workout_tracker.social.client.NotificationClient;
import com.workout_tracker.social.dto.UserSummaryDto;
import com.workout_tracker.social.exception.BusinessRuleViolationException;
import com.workout_tracker.social.exception.ResourceNotFoundException;
import com.workout_tracker.social.model.Follow;
import com.workout_tracker.social.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Mirrors the monolith's SocialService but operates over Long ids instead of User
// entities. The User entity lives in the user-database which social-service has no
// access to — every "show me this user" enrichment goes through MainAppClient.
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialService {

    private final FollowRepository followRepository;
    private final MainAppClient mainAppClient;
    private final NotificationClient notificationClient;

    @Transactional
    public void follow(Long followerId, String followerUsername, Long followedId) {
        if (followerId.equals(followedId)) {
            log.warn("User {} attempted to follow themselves", followerId);
            throw new BusinessRuleViolationException("You cannot follow yourself");
        }

        // Verify the target user exists by going through MainAppClient. If the breaker
        // is open we still get a "Unknown" placeholder back — accept that and let the
        // unique-constraint plus follower-side check guard against bad data. We do NOT
        // want a degraded monolith to lock out all follow operations.
        UserSummaryDto target = mainAppClient.getUser(followedId);
        if (target == null) {
            throw ResourceNotFoundException.of("User", followedId);
        }

        if (followRepository.existsByFollowerIdAndFollowedId(followerId, followedId)) {
            log.warn("User {} attempted duplicate follow of {}", followerId, followedId);
            throw new BusinessRuleViolationException("You already follow this user");
        }

        followRepository.save(Follow.builder()
                .followerId(followerId)
                .followedId(followedId)
                .build());
        log.info("User {} now follows {}", followerId, followedId);

        // Best-effort notification to the followed user. Fire AFTER save so a
        // breaker-open downstream can't block the follow itself.
        notificationClient.notifyNewFollower(followedId, followerUsername);
    }

    @Transactional
    public void unfollow(Long followerId, Long followedId) {
        Follow follow = followRepository.findByFollowerIdAndFollowedId(followerId, followedId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You do not follow user " + followedId));
        followRepository.delete(follow);
        log.info("User {} unfollowed {}", followerId, followedId);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getFollowing(Long userId) {
        // N upstream calls — one per followed user. For demo scale this is fine; in a
        // production system we'd add a batch /internal/users?ids=...&ids=... endpoint
        // and Redis cache. Documented as future work.
        return followRepository.findByFollowerId(userId).stream()
                .map(Follow::getFollowedId)
                .map(mainAppClient::getUser)
                .filter(u -> u != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getFollowers(Long userId) {
        return followRepository.findByFollowedId(userId).stream()
                .map(Follow::getFollowerId)
                .map(mainAppClient::getUser)
                .filter(u -> u != null)
                .toList();
    }

    // Exposed for PostService.getFeed — keeps the "who do I follow" projection in one place.
    @Transactional(readOnly = true)
    public List<Long> getFollowedUserIds(Long userId) {
        return followRepository.findByFollowerId(userId).stream()
                .map(Follow::getFollowedId)
                .toList();
    }

    // Used by PostService.createPost to fan out POST_CREATED notifications to every
    // follower of the author.
    @Transactional(readOnly = true)
    public List<Long> getFollowerIds(Long userId) {
        return followRepository.findByFollowedId(userId).stream()
                .map(Follow::getFollowerId)
                .toList();
    }
}
