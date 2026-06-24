package com.workout_tracker.notification.controller;

import com.workout_tracker.notification.dto.NotificationDto;
import com.workout_tracker.notification.dto.PageResponse;
import com.workout_tracker.notification.security.AuthenticatedUser;
import com.workout_tracker.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageResponse<NotificationDto>> list(
            @AuthenticationPrincipal AuthenticatedUser current,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(
                notificationService.list(current.userId(), pageable),
                Function.identity()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @AuthenticationPrincipal AuthenticatedUser current) {
        return ResponseEntity.ok(Map.of(
                "count", notificationService.unreadCount(current.userId())));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markRead(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser current) {
        return ResponseEntity.ok(notificationService.markRead(id, current.userId()));
    }
}
