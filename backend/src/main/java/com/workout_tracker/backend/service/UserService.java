package com.workout_tracker.backend.service;

import com.workout_tracker.backend.dto.user.UserProfileDto;
import com.workout_tracker.backend.dto.user.UserProfileUpdateRequest;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.UserProfile;
import com.workout_tracker.backend.repository.UserProfileRepository;
import com.workout_tracker.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Profile read/update + public user lookups for the social layer.
// Authentication (register/login) lives in AuthService; this service handles
// everything that operates on an already-authenticated User or on user lookups
// by id (e.g. follow target, post author).
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    // Reads the profile row directly (one query) and pairs it with the auth-principal
    // User the caller already holds. Avoids reloading the User just to trigger the
    // OneToOne lazy fetch through the back-reference.
    @Transactional(readOnly = true)
    public UserProfileDto getMyProfile(User user) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        return SocialMapper.toUserProfileDto(user, profile);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(Long userId) {
        User loaded = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        return SocialMapper.toUserProfileDto(loaded);
    }

    // PATCH-style update: only fields present (non-null) on the request overwrite
    // existing values. Validation ranges (height 50–300, weight 20–500) live on the
    // request record, so anything that lands here is already in-range.
    @Transactional
    public UserProfileDto updateMyProfile(User user, UserProfileUpdateRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    // Auth.register seeds a UserProfile, so this branch is mostly defensive
                    // for older rows or tests that build users directly.
                    log.warn("User {} had no UserProfile row; creating one", user.getUsername());
                    return userProfileRepository.save(new UserProfile(user));
                });

        if (request.bio() != null) profile.setBio(request.bio());
        if (request.fitnessGoal() != null) profile.setFitnessGoal(request.fitnessGoal());
        if (request.heightCm() != null) profile.setHeightCm(request.heightCm());
        if (request.weightKg() != null) profile.setWeightKg(request.weightKg());
        if (request.gender() != null) profile.setGender(request.gender());

        // Profile mutations are dirty-checked on commit. Build the response from the
        // local user + profile we already hold — no reload, no lazy fetch.
        log.info("UserProfile updated for user {}", user.getUsername());
        return SocialMapper.toUserProfileDto(user, profile);
    }

    // Loads a User by id and throws 404 if missing — used by SocialService and PostService
    // when resolving a target user from a path variable.
    User loadUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }
}
