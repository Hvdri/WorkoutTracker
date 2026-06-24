package com.workout_tracker.notification.controller;

import com.workout_tracker.notification.dto.CreateNotificationRequest;
import com.workout_tracker.notification.dto.NotificationDto;
import com.workout_tracker.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Inter-service endpoint. Same posture as the monolith's /internal/ controller —
// no JWT required because in a deployed environment this is only reachable from
// inside the Docker network. The monolith's NotificationClient hits this fire-
// and-forget after a workout log is marked COMPLETED.
@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationDto> create(@Valid @RequestBody CreateNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.create(request));
    }
}
