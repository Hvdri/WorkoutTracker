package com.workout_tracker.notification.service;

import com.workout_tracker.notification.dto.CreateNotificationRequest;
import com.workout_tracker.notification.dto.NotificationDto;
import com.workout_tracker.notification.exception.ResourceNotFoundException;
import com.workout_tracker.notification.model.Notification;
import com.workout_tracker.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // Called by the monolith via /internal/ when an interesting event happens
    // (workout completed, eventually: post created, user followed, etc).
    public NotificationDto create(CreateNotificationRequest request) {
        Notification n = Notification.builder()
                .userId(request.userId())
                .type(request.type())
                .message(request.message())
                .workoutLogId(request.workoutLogId())
                .createdAt(LocalDateTime.now())
                .read(false)
                .build();
        Notification saved = notificationRepository.save(n);
        log.info("Notification {} created for user {} type={}", saved.getId(),
                saved.getUserId(), saved.getType());
        return toDto(saved);
    }

    public Page<NotificationDto> list(Long userId, Pageable pageable) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationService::toDto);
    }

    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public NotificationDto markRead(String id, Long currentUserId) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", id));
        // Match the monolith's posture — wrong owner is also 404 to avoid leaking ids.
        if (!n.getUserId().equals(currentUserId)) {
            log.warn("User {} attempted to mark Notification {} owned by {}",
                    currentUserId, id, n.getUserId());
            throw ResourceNotFoundException.of("Notification", id);
        }
        if (!n.isRead()) {
            n.setRead(true);
            notificationRepository.save(n);
        }
        return toDto(n);
    }

    private static NotificationDto toDto(Notification n) {
        return new NotificationDto(
            n.getId(), n.getUserId(), n.getType(), n.getMessage(),
            n.getWorkoutLogId(), n.getCreatedAt(), n.isRead()
        );
    }
}
