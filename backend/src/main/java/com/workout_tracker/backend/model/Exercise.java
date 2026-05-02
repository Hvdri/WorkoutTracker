package com.workout_tracker.backend.model;

import com.workout_tracker.backend.model.enums.MuscleGroup;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

// Class-level @BatchSize batches all lazy loads of Exercise across the same session,
// turning the per-ExerciseLog .getExercise() N+1 on /api/logs into a single batched IN query.
@Entity
@Table(name = "exercises")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@BatchSize(size = 50)
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MuscleGroup muscleGroup;

    private String imageUrl;
}
