package com.workout_tracker.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "set_logs",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_setlog_exerciselog_setnumber",
        columnNames = {"exercise_log_id", "set_number"}
    )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SetLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int setNumber;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(nullable = false)
    private int reps;

    private Integer rpe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_log_id", nullable = false)
    private ExerciseLog exerciseLog;
}
