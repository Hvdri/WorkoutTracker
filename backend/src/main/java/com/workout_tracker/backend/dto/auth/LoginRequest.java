package com.workout_tracker.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password,

        // Optional. When true, the issued JWT uses the longer remember-me expiry
        // (~30 days) so the user stays signed in across browser restarts. Null /
        // false → standard 24h expiry. localStorage persistence is the same either
        // way; only the token's exp claim changes.
        Boolean rememberMe
) {}
