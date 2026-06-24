package com.workout_tracker.backend.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

// Binds the `app.jwt` block in application.yaml into this object.
// @Validated makes Spring enforce the constraints below at startup — fail fast
// with a clear error rather than a cryptic decode failure later.
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Validated
@Getter @Setter
public class JwtProperties {

    @NotBlank(message = "app.jwt.secret is required. Set the JWT_SECRET environment variable " +
                        "to a Base64-encoded string of at least 32 bytes.")
    private String secret;

    @Min(value = 60_000, message = "app.jwt.expiration-ms must be at least 60000 (1 minute).")
    private long expirationMs;

    // Longer expiry used when the login request sets rememberMe=true. Default
    // 30 days matches what most "stay signed in" checkboxes do in practice.
    // Configurable via app.jwt.remember-me-expiration-ms.
    @Min(value = 60_000, message = "app.jwt.remember-me-expiration-ms must be at least 60000 (1 minute).")
    private long rememberMeExpirationMs = 30L * 24 * 60 * 60 * 1000;
}
