package com.workout_tracker.backend.model;

import com.workout_tracker.backend.model.enums.Gender;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

// Class-level @BatchSize batches lazy UserProfile fetches across followers/following
// list rendering — without it, a list of N follows triggers N profile SELECTs.
// Mirrors the existing @BatchSize on WorkoutLog and WorkoutTemplate.
@Entity
@Table(name = "user_profiles")
@BatchSize(size = 20)
@Getter @Setter @NoArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Double heightCm;
    private Double weightKg;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(length = 500)
    private String bio;

    private String fitnessGoal;

    public UserProfile(User user) {
        this.user = user;
    }
}