package com.workout_tracker.social.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// In the monolith, follower/followed are @ManyToOne to User. In social-service we own
// neither the User entity nor its database, so we store the IDs as bare Longs.
// Referential integrity to the user table is now the application's responsibility:
// SocialService never inserts a row without first verifying both ids via MainAppClient.
@Entity
@Table(
    name = "follows",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_follower_followed",
        columnNames = {"follower_id", "followed_id"}
    )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "followed_id", nullable = false)
    private Long followedId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
