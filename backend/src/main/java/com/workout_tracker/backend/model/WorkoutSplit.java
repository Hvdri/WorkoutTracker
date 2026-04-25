package com.workout_tracker.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workout_splits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "split", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkoutTemplate> templates = new ArrayList<>();
}
