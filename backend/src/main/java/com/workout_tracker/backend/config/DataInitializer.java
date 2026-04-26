package com.workout_tracker.backend.config;

import com.workout_tracker.backend.model.Exercise;
import com.workout_tracker.backend.model.Role;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.UserProfile;
import com.workout_tracker.backend.model.enums.MuscleGroup;
import com.workout_tracker.backend.repository.ExerciseRepository;
import com.workout_tracker.backend.repository.RoleRepository;
import com.workout_tracker.backend.repository.UserProfileRepository;
import com.workout_tracker.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Runs once after the application has fully started.
// Seeds:
//   - the two roles (so AuthService.register can do orElseThrow safely)
//   - one ADMIN user (so the exercise admin endpoints are reachable on a fresh DB)
//   - the global Exercise catalog (≥ 20 entries across all MuscleGroup values)
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ExerciseRepository exerciseRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.email:admin@workouttracker.com}")
    private String adminEmail;

    // No @Value default: the only intentional dev/test fallback lives in application-dev.yaml
    // and application-test.yaml. In prod-without-ADMIN_PASSWORD the placeholder in
    // application.yaml fails to resolve and startup errors out — which is the goal.
    @Value("${app.admin.password}")
    private String adminPassword;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        seedRoles();
        seedAdminUser();
        seedExerciseCatalog();
    }

    private void seedRoles() {
        createRoleIfMissing(Role.ROLE_USER);
        createRoleIfMissing(Role.ROLE_ADMIN);
    }

    private void createRoleIfMissing(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            roleRepository.save(new Role(roleName));
            log.info("Seeded role: {}", roleName);
        }
    }

    private void seedAdminUser() {
        // Skip if either the username or the email is already taken — both columns
        // are unique on the users table, so checking only one risks a 500 on save().
        if (userRepository.existsByUsername(adminUsername) || userRepository.existsByEmail(adminEmail)) {
            return;
        }

        Role userRole = roleRepository.findByName(Role.ROLE_USER).orElseThrow();
        Role adminRole = roleRepository.findByName(Role.ROLE_ADMIN).orElseThrow();

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setEnabled(true);
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        roles.add(adminRole);
        admin.setRoles(roles);
        userRepository.save(admin);

        // UserProfile owns the FK (mappedBy = "user" on User), so persist it directly.
        // We don't set the in-memory back-reference: nothing in this transaction reads
        // admin.getProfile() afterward, and the next request will reload via LAZY fetch.
        userProfileRepository.save(new UserProfile(admin));

        log.info("Seeded admin user: {}", adminUsername);
    }

    // The exercise list below is the minimum 20-exercise catalog described in ProjectContext.md.
    // Each entry is name + muscle group. Descriptions and image URLs can be added by an admin later.
    private void seedExerciseCatalog() {
        // Per-name idempotency on purpose: if an admin deletes a single seeded exercise
        // it gets restored on the next boot, and a partially-customized catalog doesn't
        // permanently disable seeding. 30 indexed lookups on startup is negligible.
        List<Exercise> exercises = List.of(
                ex("Bench Press", MuscleGroup.CHEST),
                ex("Incline Dumbbell Press", MuscleGroup.CHEST),
                ex("Cable Fly", MuscleGroup.CHEST),
                ex("Dips", MuscleGroup.CHEST),
                ex("Pull-Up", MuscleGroup.BACK),
                ex("Barbell Row", MuscleGroup.BACK),
                ex("Lat Pulldown", MuscleGroup.BACK),
                ex("Cable Row", MuscleGroup.BACK),
                ex("Overhead Press", MuscleGroup.SHOULDERS),
                ex("Lateral Raise", MuscleGroup.SHOULDERS),
                ex("Face Pull", MuscleGroup.SHOULDERS),
                ex("Arnold Press", MuscleGroup.SHOULDERS),
                ex("Barbell Curl", MuscleGroup.BICEPS),
                ex("Hammer Curl", MuscleGroup.BICEPS),
                ex("Incline Curl", MuscleGroup.BICEPS),
                ex("Tricep Pushdown", MuscleGroup.TRICEPS),
                ex("Skull Crusher", MuscleGroup.TRICEPS),
                ex("Overhead Extension", MuscleGroup.TRICEPS),
                ex("Squat", MuscleGroup.LEGS),
                ex("Romanian Deadlift", MuscleGroup.LEGS),
                ex("Leg Press", MuscleGroup.LEGS),
                ex("Leg Curl", MuscleGroup.LEGS),
                ex("Hip Thrust", MuscleGroup.GLUTES),
                ex("Bulgarian Split Squat", MuscleGroup.GLUTES),
                ex("Plank", MuscleGroup.CORE),
                ex("Cable Crunch", MuscleGroup.CORE),
                ex("Hanging Leg Raise", MuscleGroup.CORE),
                ex("Treadmill Run", MuscleGroup.CARDIO),
                ex("Stationary Bike", MuscleGroup.CARDIO),
                ex("Rowing Machine", MuscleGroup.CARDIO)
        );

        int inserted = 0;
        for (Exercise candidate : exercises) {
            if (!exerciseRepository.existsByNameIgnoreCase(candidate.getName())) {
                exerciseRepository.save(candidate);
                inserted++;
            }
        }
        if (inserted > 0) {
            log.info("Seeded {} exercises (skipped {} already present)", inserted, exercises.size() - inserted);
        }
    }

    private static Exercise ex(String name, MuscleGroup group) {
        return Exercise.builder().name(name).muscleGroup(group).build();
    }
}
