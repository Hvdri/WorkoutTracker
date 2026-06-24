package com.workout_tracker.backend.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

// Fire-and-forget client into notification-service. Mirrors the shape of social-
// service's MainAppClient (the prof's DiscountClient pattern):
//
//   @CircuitBreaker(name = "notification-service", fallbackMethod = "...")
//   public void notify(...) { restClient.post()... }
//   private void notifyFallback(..., Throwable cause) { log.warn(...); }
//
// Notifications are non-critical: if Mongo is down or the service is unreachable,
// completing a workout still has to succeed. The fallback just logs and swallows.
// Together with social-service's MainAppClient this satisfies the spec's
// "Circuit Breaker for minimum 2 services" requirement.
@Component
@Slf4j
public class NotificationClient {

    private static final String CB_NAME = "notification-service";

    private final RestClient notificationRestClient;

    public NotificationClient(@Value("${app.notification-service.uri}") String baseUri) {
        // Use the static builder rather than injecting RestClient.Builder — same
        // reason as social-service's RestClientConfig, Boot 4's auto-config doesn't
        // always surface the Builder bean for non-actuator slices.
        this.notificationRestClient = RestClient.builder().baseUrl(baseUri).build();
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "notifyWorkoutCompletedFallback")
    public void notifyWorkoutCompleted(Long userId, Long workoutLogId, String templateName) {
        notificationRestClient.post()
                .uri("/internal/notifications")
                .body(Map.of(
                    "userId", userId,
                    "type", "WORKOUT_COMPLETED",
                    "message", "You completed your " + templateName + " workout. Nice work!",
                    "workoutLogId", workoutLogId
                ))
                .retrieve()
                .toBodilessEntity();
    }

    // Signature must match the protected method plus a trailing Throwable so Resilience4j
    // can route to it. Returns void — the caller in WorkoutLogService doesn't await any
    // result; the notification is best-effort.
    @SuppressWarnings("unused")
    private void notifyWorkoutCompletedFallback(Long userId, Long workoutLogId,
                                                String templateName, Throwable cause) {
        log.warn("Notification fire failed (user={}, log={}): {} — swallowing, completion still succeeded",
                userId, workoutLogId, cause.toString());
    }
}
