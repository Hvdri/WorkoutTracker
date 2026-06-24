package com.workout_tracker.social.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

// Outbound client to notification-service. Same shape as MainAppClient and the
// monolith's NotificationClient — annotation-based circuit breaker, fire-and-forget
// fallback that just logs.
//
// social-service publishes two notification kinds:
//   * NEW_FOLLOWER  — fired from SocialService.follow, targets the followed user
//   * POST_CREATED  — fired from PostService.createPost, fans out to every follower
//                     of the author
//
// Both are best-effort. If notification-service is down or its breaker is open,
// follow / share-workout still succeeds; the fallback just logs the dropped fire.
@Component
@Slf4j
public class NotificationClient {

    private static final String CB_NAME = "notification-service";

    private final RestClient notificationRestClient;

    public NotificationClient(@Value("${app.notification-service.uri}") String baseUri) {
        this.notificationRestClient = RestClient.builder().baseUrl(baseUri).build();
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "notifyNewFollowerFallback")
    public void notifyNewFollower(Long followedUserId, String followerUsername) {
        notificationRestClient.post()
                .uri("/internal/notifications")
                .body(Map.of(
                    "userId", followedUserId,
                    "type", "NEW_FOLLOWER",
                    "message", "@" + followerUsername + " started following you."
                ))
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unused")
    private void notifyNewFollowerFallback(Long followedUserId, String followerUsername,
                                           Throwable cause) {
        log.warn("Notification fire failed (NEW_FOLLOWER target={} from={}): {} — swallowing",
                followedUserId, followerUsername, cause.toString());
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "notifyPostCreatedFallback")
    public void notifyPostCreated(Long followerUserId, String authorUsername, Long workoutLogId) {
        notificationRestClient.post()
                .uri("/internal/notifications")
                .body(Map.of(
                    "userId", followerUserId,
                    "type", "POST_CREATED",
                    "message", "@" + authorUsername + " shared a workout.",
                    "workoutLogId", workoutLogId
                ))
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unused")
    private void notifyPostCreatedFallback(Long followerUserId, String authorUsername,
                                           Long workoutLogId, Throwable cause) {
        log.warn("Notification fire failed (POST_CREATED target={} from={} log={}): {} — swallowing",
                followerUserId, authorUsername, workoutLogId, cause.toString());
    }
}
