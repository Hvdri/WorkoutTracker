package com.workout_tracker.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exercise_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExerciseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_log_id", nullable = false)
    private WorkoutLog workoutLog;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @OneToMany(mappedBy = "exerciseLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SetLog> sets = new ArrayList<>();
}
