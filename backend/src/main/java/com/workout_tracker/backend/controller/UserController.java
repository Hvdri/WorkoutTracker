package com.workout_tracker.backend.controller;

import com.workout_tracker.backend.dto.common.PageResponse;
import com.workout_tracker.backend.dto.social.PostDto;
import com.workout_tracker.backend.dto.user.UserProfileDto;
import com.workout_tracker.backend.dto.user.UserProfileUpdateRequest;
import com.workout_tracker.backend.dto.user.UserSummaryDto;
import com.workout_tracker.backend.service.CurrentUserService;
import com.workout_tracker.backend.service.PostService;
import com.workout_tracker.backend.service.SocialService;
import com.workout_tracker.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PostService postService;
    private final SocialService socialService;
    private final CurrentUserService currentUserService;

    @GetMapping("/me/profile")
    public ResponseEntity<UserProfileDto> getMyProfile(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(userService.getMyProfile(currentUserService.get(principal)));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<UserProfileDto> updateMyProfile(
            @Valid @RequestBody UserProfileUpdateRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                userService.updateMyProfile(currentUserService.get(principal), request));
    }

    // Public-but-authenticated read of another user's profile (used on /users/:id pages).
    @GetMapping("/{id}/profile")
    public ResponseEntity<UserProfileDto> getUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    @GetMapping("/{id}/posts")
    public ResponseEntity<PageResponse<PostDto>> getUserPosts(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(postService.getUserPosts(id, pageable)));
    }

    // Public-by-id social-graph reads. Used on /users/:id to show follower/following
    // counts (frontend uses .length). 404 if the user does not exist.
    @GetMapping("/{id}/followers")
    public ResponseEntity<List<UserSummaryDto>> getUserFollowers(@PathVariable Long id) {
        return ResponseEntity.ok(socialService.getFollowersOf(id));
    }

    @GetMapping("/{id}/following")
    public ResponseEntity<List<UserSummaryDto>> getUserFollowing(@PathVariable Long id) {
        return ResponseEntity.ok(socialService.getFollowingOf(id));
    }
}
