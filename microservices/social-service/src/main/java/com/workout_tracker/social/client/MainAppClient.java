package com.workout_tracker.social.client;

import com.workout_tracker.social.dto.UserSummaryDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

// Client into the monolith's /internal/ projections. Modeled on the DiscountClient
// from awbd2026/product-hub/product-api-app:
//
//   @CircuitBreaker(name = "discount-service", fallbackMethod = "getDiscountFallback")
//   public Discount getDiscount(Long productId) { restClient.get()... }
//   private Discount getDiscountFallback(Long productId, Throwable cause) {
//       return Discount.unavailable(productId);
//   }
//
// Same shape, same wrapper. Sliding-window + open-state config lives in
// application.properties under resilience4j.circuitbreaker.instances.main-app.*.
//
// Fallbacks return a degraded sentinel for READ-path enrichment (the feed still
// renders with "Unknown" usernames if the monolith is down). Callers that depend on
// the data being authoritative — specifically PostService.createPost, which uses
// LogSummary to gate post creation on owner+status — must check LogSummary.isDegraded()
// and throw UpstreamUnavailableException instead.
@Component
@RequiredArgsConstructor
@Slf4j
public class MainAppClient {

    private static final String CB_NAME = "main-app";

    private final RestClient mainAppRestClient;

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "getUserFallback")
    public UserSummaryDto getUser(Long userId) {
        return mainAppRestClient.get()
                .uri("/internal/users/{id}", userId)
                .retrieve()
                .body(UserSummaryDto.class);
    }

    // Signature must match the protected method PLUS a trailing Throwable.
    // Resilience4j picks the most-specific fallback; an unused method = silent miss.
    private UserSummaryDto getUserFallback(Long userId, Throwable cause) {
        log.warn("MainApp getUser({}) → fallback: {}", userId, cause.toString());
        return new UserSummaryDto(userId, "Unknown", null);
    }

    // 404 from the monolith means the user genuinely doesn't exist — we want to surface
    // that as a 404 to the social-service caller, NOT trigger the circuit breaker.
    // RestClientResponseException is wrapped, so we let it bubble; @CircuitBreaker counts
    // only `Exception` instances toward the failure rate window — a 404 still counts as
    // a failure. For a stricter setup we'd configure `ignoreExceptions` on the breaker,
    // out of scope for this pass.

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "getLogSummaryFallback")
    public LogSummary getLogSummary(Long logId) {
        try {
            return mainAppRestClient.get()
                    .uri("/internal/logs/{id}/summary", logId)
                    .retrieve()
                    .body(LogSummary.class);
        } catch (RestClientResponseException ex) {
            // 404 from monolith = log doesn't exist. Don't trip the breaker on legit
            // not-found — let the service surface a proper 404 to the user.
            if (ex.getStatusCode().value() == 404) {
                return null;
            }
            throw ex;
        }
    }

    private LogSummary getLogSummaryFallback(Long logId, Throwable cause) {
        log.warn("MainApp getLogSummary({}) → fallback: {}", logId, cause.toString());
        return LogSummary.unavailable(logId);
    }
}
