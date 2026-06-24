package com.workout_tracker.backend.controller;

import com.workout_tracker.backend.dto.internal.LogSummaryDto;
import com.workout_tracker.backend.dto.user.UserSummaryDto;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.WorkoutLog;
import com.workout_tracker.backend.repository.UserRepository;
import com.workout_tracker.backend.repository.WorkoutLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

// Inter-service endpoints under /internal/ — never routed through the public API gateway
// in production. Other Spring Boot services (social-service, future notification-service)
// hit these for cross-domain projections they don't own. SecurityConfig permits these
// without JWT because in a deployed environment they're only reachable from inside the
// Docker network. For an extra hardening pass we could check a shared header secret;
// out of scope for this project.
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final UserRepository userRepository;
    private final WorkoutLogRepository workoutLogRepository;

    // Both methods are @Transactional(readOnly=true) because they walk lazy
    // associations (user.profile, log.user, log.template) and we need the JPA
    // session open through the field access. Without this, Hibernate throws
    // LazyInitializationException at JSON serialization time — `open-in-view` is
    // disabled in this app for the usual reasons (predictable transaction scope,
    // no surprise queries during view rendering).
    @GetMapping("/users/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<UserSummaryDto> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
        String bio = user.getProfile() != null ? user.getProfile().getBio() : null;
        return ResponseEntity.ok(new UserSummaryDto(user.getId(), user.getUsername(), bio));
    }

    @GetMapping("/logs/{id}/summary")
    @Transactional(readOnly = true)
    public ResponseEntity<LogSummaryDto> getLogSummary(@PathVariable Long id) {
        WorkoutLog logEntity = workoutLogRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("WorkoutLog", id));
        return ResponseEntity.ok(new LogSummaryDto(
                logEntity.getId(),
                logEntity.getUser().getId(),
                logEntity.getStatus().name(),
                logEntity.getTemplate().getName()
        ));
    }
}
