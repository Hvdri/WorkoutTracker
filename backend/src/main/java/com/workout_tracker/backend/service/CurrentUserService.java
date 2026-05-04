package com.workout_tracker.backend.service;

import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

// Resolves the full User entity from the JWT principal. Centralized here so every
// controller doesn't reach into UserRepository directly. If the principal exists but
// the row is gone, it's a server-side invariant violation — let it crash to 500.
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User get(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user '" + principal.getUsername() + "' not found in database"));
    }
}
