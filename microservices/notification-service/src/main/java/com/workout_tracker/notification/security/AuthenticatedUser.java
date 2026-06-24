package com.workout_tracker.notification.security;

import java.util.List;

// What we stash as the SecurityContext principal after a valid JWT lands. Controllers
// pull this out with @AuthenticationPrincipal — userId is the bit the social services
// care about, username is kept for logging.
public record AuthenticatedUser(Long userId, String username, List<String> roles) {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
