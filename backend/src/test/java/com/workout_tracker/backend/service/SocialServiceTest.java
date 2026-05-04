package com.workout_tracker.backend.service;

import com.workout_tracker.backend.exception.BusinessRuleViolationException;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.Follow;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.repository.FollowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialServiceTest {

    @Mock FollowRepository followRepository;
    @Mock UserService userService;

    @InjectMocks SocialService socialService;

    @Test
    void follow_self_throwsBusinessRuleViolation() {
        // Rule #4: a user cannot follow themselves. The check must short-circuit before
        // any repository call so we don't even resolve the target user.
        User self = userWithId(1L);

        assertThatThrownBy(() -> socialService.follow(self, 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("yourself");

        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_alreadyFollowing_throwsBusinessRuleViolation() {
        // Pre-check beats the unique-constraint backstop so the client gets a domain
        // message instead of a generic 409 from DataIntegrityViolation.
        User follower = userWithId(1L);
        User followed = userWithId(2L);
        when(userService.loadUser(2L)).thenReturn(followed);
        when(followRepository.existsByFollowerAndFollowed(follower, followed)).thenReturn(true);

        assertThatThrownBy(() -> socialService.follow(follower, 2L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already");

        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_newRelationship_persists() {
        User follower = userWithId(1L);
        User followed = userWithId(2L);
        when(userService.loadUser(2L)).thenReturn(followed);
        when(followRepository.existsByFollowerAndFollowed(follower, followed)).thenReturn(false);

        socialService.follow(follower, 2L);

        verify(followRepository).save(any(Follow.class));
    }

    @Test
    void unfollow_notFollowing_throwsResourceNotFound() {
        User follower = userWithId(1L);
        User followed = userWithId(2L);
        when(userService.loadUser(2L)).thenReturn(followed);
        when(followRepository.findByFollowerAndFollowed(follower, followed))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> socialService.unfollow(follower, 2L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(followRepository, never()).delete(any());
    }

    @Test
    void unfollow_existingRelationship_deletes() {
        User follower = userWithId(1L);
        User followed = userWithId(2L);
        Follow follow = Follow.builder().id(99L).follower(follower).followed(followed).build();
        when(userService.loadUser(2L)).thenReturn(followed);
        when(followRepository.findByFollowerAndFollowed(follower, followed))
                .thenReturn(Optional.of(follow));

        socialService.unfollow(follower, 2L);

        verify(followRepository).delete(follow);
    }

    private static User userWithId(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        return u;
    }
}
