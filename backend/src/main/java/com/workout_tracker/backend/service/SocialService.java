package com.workout_tracker.backend.service;

import com.workout_tracker.backend.dto.user.UserSummaryDto;
import com.workout_tracker.backend.exception.BusinessRuleViolationException;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.Follow;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialService {

    private final FollowRepository followRepository;
    private final UserService userService;

    // Business rule #4: a user cannot follow themselves. Duplicate follows also throw 409 —
    // the unique constraint on (follower_id, followed_id) is the backstop, but pre-checking
    // gives a domain-specific message instead of a generic constraint violation.
    @Transactional
    public void follow(User follower, Long followedId) {
        if (follower.getId().equals(followedId)) {
            log.warn("User {} attempted to follow themselves", follower.getUsername());
            throw new BusinessRuleViolationException("You cannot follow yourself");
        }

        User followed = userService.loadUser(followedId);

        if (followRepository.existsByFollowerAndFollowed(follower, followed)) {
            log.warn("User {} attempted duplicate follow of {}",
                    follower.getUsername(), followed.getUsername());
            throw new BusinessRuleViolationException("You already follow this user");
        }

        followRepository.save(Follow.builder().follower(follower).followed(followed).build());
        log.info("User {} now follows {}", follower.getUsername(), followed.getUsername());
    }

    @Transactional
    public void unfollow(User follower, Long followedId) {
        User followed = userService.loadUser(followedId);
        Follow follow = followRepository.findByFollowerAndFollowed(follower, followed)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You do not follow user " + followedId));
        followRepository.delete(follow);
        log.info("User {} unfollowed {}", follower.getUsername(), followed.getUsername());
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getFollowing(User user) {
        return followRepository.findByFollower(user).stream()
                .map(Follow::getFollowed)
                .map(SocialMapper::toUserSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getFollowers(User user) {
        return followRepository.findByFollowed(user).stream()
                .map(Follow::getFollower)
                .map(SocialMapper::toUserSummaryDto)
                .toList();
    }

    // Public-by-id variants used on /users/:id pages so anyone can see another user's
    // social graph counts. Throws 404 if the user doesn't exist (consistent with
    // /api/users/{id}/profile behavior).
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getFollowingOf(Long userId) {
        return getFollowing(userService.loadUser(userId));
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getFollowersOf(Long userId) {
        return getFollowers(userService.loadUser(userId));
    }
}
