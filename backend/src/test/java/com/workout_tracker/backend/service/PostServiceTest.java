package com.workout_tracker.backend.service;

import com.workout_tracker.backend.dto.social.CreatePostRequest;
import com.workout_tracker.backend.exception.BusinessRuleViolationException;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.Post;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.WorkoutLog;
import com.workout_tracker.backend.model.WorkoutTemplate;
import com.workout_tracker.backend.model.enums.WorkoutStatus;
import com.workout_tracker.backend.repository.FollowRepository;
import com.workout_tracker.backend.repository.PostRepository;
import com.workout_tracker.backend.repository.WorkoutLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock PostRepository postRepository;
    @Mock WorkoutLogRepository workoutLogRepository;
    @Mock FollowRepository followRepository;
    @Mock UserService userService;

    @InjectMocks PostService postService;

    @Test
    void createPost_logNotOwnedByUser_throwsResourceNotFound() {
        // findByIdAndUser returns empty when the log doesn't belong to the caller.
        // We surface that as 404 (not 403) — same convention as the rest of the codebase
        // (rule #2 in ProjectContext: don't leak existence to non-owners).
        User user = userWithId(1L);
        when(workoutLogRepository.findByIdAndUser(50L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                postService.createPost(user, new CreatePostRequest(50L, "nice")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(postRepository, never()).save(any());
    }

    @Test
    void createPost_logNotCompleted_throwsBusinessRuleViolation() {
        // Only COMPLETED logs are shareable — IN_PROGRESS would expose mid-workout state.
        User user = userWithId(1L);
        WorkoutLog inProgress = logWithStatus(50L, user, WorkoutStatus.IN_PROGRESS);
        when(workoutLogRepository.findByIdAndUser(50L, user)).thenReturn(Optional.of(inProgress));

        assertThatThrownBy(() ->
                postService.createPost(user, new CreatePostRequest(50L, "nice")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("completed");

        verify(postRepository, never()).save(any());
    }

    @Test
    void createPost_duplicateForSameLog_throwsBusinessRuleViolation() {
        // Post.workoutLog is unique; pre-check gives a clean domain message instead of a
        // DataIntegrityViolation backstop.
        User user = userWithId(1L);
        WorkoutLog completed = logWithStatus(50L, user, WorkoutStatus.COMPLETED);
        when(workoutLogRepository.findByIdAndUser(50L, user)).thenReturn(Optional.of(completed));
        when(postRepository.existsByWorkoutLog(completed)).thenReturn(true);

        assertThatThrownBy(() ->
                postService.createPost(user, new CreatePostRequest(50L, "second time")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already");

        verify(postRepository, never()).save(any());
    }

    @Test
    void createPost_validCompletedLog_persists() {
        User user = userWithId(1L);
        WorkoutLog completed = logWithStatus(50L, user, WorkoutStatus.COMPLETED);
        when(workoutLogRepository.findByIdAndUser(50L, user)).thenReturn(Optional.of(completed));
        when(postRepository.existsByWorkoutLog(completed)).thenReturn(false);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });

        postService.createPost(user, new CreatePostRequest(50L, "felt good"));

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getCaption()).isEqualTo("felt good");
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getWorkoutLog()).isEqualTo(completed);
    }

    @Test
    void deletePost_wrongOwner_throwsResourceNotFound() {
        // Same 404-leak-protection convention as WorkoutSplitService.loadOwnedSplit.
        User attacker = userWithId(2L);
        Post post = Post.builder().id(99L).user(userWithId(1L)).caption("x").build();
        when(postRepository.findById(99L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(99L, attacker))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(postRepository, never()).delete(any());
    }

    private static User userWithId(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        return u;
    }

    private static WorkoutLog logWithStatus(Long id, User user, WorkoutStatus status) {
        WorkoutTemplate template = WorkoutTemplate.builder().id(99L).name("Push").build();
        return WorkoutLog.builder()
                .id(id).user(user).template(template)
                .date(LocalDate.now()).status(status).build();
    }
}
