package com.workout_tracker.social.client;

// Wire-shape mirror of the monolith's LogSummaryDto on /internal/logs/{id}/summary.
// A separate type in this service so monolith DTO changes don't break our deserializer
// silently — Jackson tolerates unknown fields by default.
//
// `status` is a String (not an enum) so that the fallback can emit a sentinel value
// ("UNKNOWN") without needing to define an extra enum constant.
public record LogSummary(
    Long id,
    Long ownerId,
    String status,
    String templateName
) {
    public static LogSummary unavailable(Long id) {
        return new LogSummary(id, null, "UNKNOWN", "Unknown");
    }

    public boolean isDegraded() {
        return "UNKNOWN".equals(status);
    }
}
