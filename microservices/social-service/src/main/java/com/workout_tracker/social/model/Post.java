package com.workout_tracker.social.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// Severed from User and WorkoutLog — neither entity lives in social-service.
// userId points at the monolith's users.id; workoutLogId points at workout_logs.id.
// workoutLogId is unique here too (one post per log, same rule as the monolith).
@Entity
@Table(name = "posts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 300)
    private String caption;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "workout_log_id", nullable = false, unique = true)
    private Long workoutLogId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
