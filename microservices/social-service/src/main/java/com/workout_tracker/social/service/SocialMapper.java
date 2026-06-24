package com.workout_tracker.social.service;

import com.workout_tracker.social.client.LogSummary;
import com.workout_tracker.social.dto.PostDto;
import com.workout_tracker.social.dto.UserSummaryDto;
import com.workout_tracker.social.model.Post;

class SocialMapper {

    private SocialMapper() {}

    // Enrichment requires both upstream calls — the entity itself only has ids.
    static PostDto toPostDto(Post post, UserSummaryDto author, LogSummary logSummary) {
        return new PostDto(
            post.getId(),
            post.getUserId(),
            author != null ? author.username() : "Unknown",
            post.getWorkoutLogId(),
            logSummary != null ? logSummary.templateName() : "Unknown",
            post.getCaption(),
            post.getCreatedAt()
        );
    }
}
