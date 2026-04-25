package com.workout_tracker.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exercise_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExerciseTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer targetSets;

    private Integer targetReps;

    @Column(nullable = false)
    @Builder.Default
    private int orderIndex = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private WorkoutTemplate template;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;
}
