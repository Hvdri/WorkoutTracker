package com.workout_tracker.backend.service;

import com.workout_tracker.backend.dto.social.PostDto;
import com.workout_tracker.backend.dto.user.UserProfileDto;
import com.workout_tracker.backend.dto.user.UserSummaryDto;
import com.workout_tracker.backend.model.Post;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.UserProfile;

// Mirror of WorkoutMapper for the social/profile slice. Kept package-private so
// services in this package can reuse it; controllers stay DTO-only.
final class SocialMapper {

    private SocialMapper() {}

    static PostDto toPostDto(Post post) {
        // workoutLog is non-optional; template chain is non-optional too. Both already
        // resolved inside the service @Transactional, so .get*() here is safe.
        return new PostDto(
                post.getId(),
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getWorkoutLog().getId(),
                post.getWorkoutLog().getTemplate().getName(),
                post.getCaption(),
                post.getCreatedAt()
        );
    }

    // Profile may be null for users created before the OneToOne was populated, or in
    // unit tests that build a User directly. Treat that as a blank profile rather
    // than NPE — the API still returns the basic identity fields.
    static UserProfileDto toUserProfileDto(User user) {
        return toUserProfileDto(user, user.getProfile());
    }

    // Two-arg variant used when the caller already holds the UserProfile in a local
    // variable (e.g. UserService.updateMyProfile after mutating it). Avoids the lazy
    // load that the one-arg overload would trigger via user.getProfile().
    static UserProfileDto toUserProfileDto(User user, UserProfile profile) {
        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getUsername(),
                profile != null ? profile.getBio() : null,
                profile != null ? profile.getFitnessGoal() : null,
                profile != null ? profile.getHeightCm() : null,
                profile != null ? profile.getWeightKg() : null,
                profile != null ? profile.getGender() : null
        );
    }

    static UserSummaryDto toUserSummaryDto(User user) {
        UserProfile profile = user.getProfile();
        return new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                profile != null ? profile.getBio() : null
        );
    }
}
