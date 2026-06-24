package com.workout_tracker.notification.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Loaded from app.jwt.secret (which itself defers to the JWT_SECRET env var shared
// with the monolith — same Base64-encoded string, same HMAC key).
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter @Setter
public class JwtProperties {
    private String secret;
}
