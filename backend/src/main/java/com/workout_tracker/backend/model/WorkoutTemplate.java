package com.workout_tracker.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workout_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private int orderIndex = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "split_id", nullable = false)
    private WorkoutSplit split;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExerciseTemplate> exerciseTemplates = new ArrayList<>();
}
