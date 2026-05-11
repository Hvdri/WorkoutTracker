package com.workout_tracker.backend.service;

import com.workout_tracker.backend.dto.user.UserProfileDto;
import com.workout_tracker.backend.dto.user.UserProfileUpdateRequest;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.UserProfile;
import com.workout_tracker.backend.model.enums.Gender;
import com.workout_tracker.backend.repository.UserProfileRepository;
import com.workout_tracker.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserProfileRepository userProfileRepository;

    @InjectMocks UserService userService;

    @Test
    void getUserProfile_notFound_throwsResourceNotFound() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateMyProfile_putSemantics_overwritesEveryField_nullClears() {
        // PUT-style: every field on the request is written through. null clears the
        // stored value so users can reset bio/goal/height/etc. via the UI. The frontend
        // always submits the full form, so unchanged fields arrive at current values.
        User user = userWithId(1L);
        UserProfile profile = new UserProfile(user);
        profile.setBio("old bio");
        profile.setFitnessGoal("old goal");
        profile.setHeightCm(180.0);
        profile.setWeightKg(75.0);
        profile.setGender(Gender.MALE);
        user.setProfile(profile);

        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        // Sets bio + weight, clears fitnessGoal + height + gender.
        UserProfileUpdateRequest req = new UserProfileUpdateRequest(
                "new bio", null, null, 80.0, null);
        UserProfileDto dto = userService.updateMyProfile(user, req);

        assertThat(profile.getBio()).isEqualTo("new bio");
        assertThat(profile.getFitnessGoal()).isNull();
        assertThat(profile.getHeightCm()).isNull();
        assertThat(profile.getWeightKg()).isEqualTo(80.0);
        assertThat(profile.getGender()).isNull();
        assertThat(dto.bio()).isEqualTo("new bio");
        assertThat(dto.weightKg()).isEqualTo(80.0);
        assertThat(dto.fitnessGoal()).isNull();
    }

    @Test
    void getMyProfile_returnsProfileFieldsForCallerUser() {
        // getMyProfile reads the profile by user_id directly and pairs it with the
        // auth-principal User the caller already holds — no User reload needed.
        User user = userWithId(1L);
        UserProfile profile = new UserProfile(user);
        profile.setBio("hello");

        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        UserProfileDto dto = userService.getMyProfile(user);

        assertThat(dto.userId()).isEqualTo(1L);
        assertThat(dto.bio()).isEqualTo("hello");
    }

    @Test
    void getMyProfile_missingProfileRow_returnsBasicIdentityFields() {
        // Defensive: if the profile row is somehow absent (legacy users, mid-migration,
        // direct test User), the response still includes id/username and null profile fields.
        User user = userWithId(7L);
        when(userProfileRepository.findByUserId(7L)).thenReturn(Optional.empty());

        UserProfileDto dto = userService.getMyProfile(user);

        assertThat(dto.userId()).isEqualTo(7L);
        assertThat(dto.username()).isEqualTo("user7");
        assertThat(dto.bio()).isNull();
    }

    private static User userWithId(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        return u;
    }
}
