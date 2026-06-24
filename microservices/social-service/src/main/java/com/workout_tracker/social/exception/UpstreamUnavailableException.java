package com.workout_tracker.social.exception;

// Thrown when MainAppClient's circuit-breaker fallback returns a degraded result for a
// call that the service can't safely degrade — specifically the log-summary lookup used
// to verify post ownership + status. Read-path calls (feed enrichment) accept the
// degraded "Unknown" username instead.
public class UpstreamUnavailableException extends RuntimeException {

    public UpstreamUnavailableException(String message) {
        super(message);
    }
}
