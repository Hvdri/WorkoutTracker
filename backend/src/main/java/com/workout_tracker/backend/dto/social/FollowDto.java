package com.workout_tracker.backend.dto.social;

public record FollowDto(
    Long followerId,
    String followerUsername,
    Long followedId,
    String followedUsername
) {}
