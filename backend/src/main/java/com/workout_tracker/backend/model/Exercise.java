package com.workout_tracker.backend.model;

import com.workout_tracker.backend.model.enums.MuscleGroup;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exercises")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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
