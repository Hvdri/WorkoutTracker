package com.workout_tracker.backend.controller;

import com.workout_tracker.backend.dto.common.PageResponse;
import com.workout_tracker.backend.dto.social.PostDto;
import com.workout_tracker.backend.dto.user.UserSummaryDto;
import com.workout_tracker.backend.service.CurrentUserService;
import com.workout_tracker.backend.service.PostService;
import com.workout_tracker.backend.service.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;
    private final PostService postService;
    private final CurrentUserService currentUserService;

    @PostMapping("/follow/{userId}")
    public ResponseEntity<Void> follow(@PathVariable Long userId,
                                       @AuthenticationPrincipal UserDetails principal) {
        socialService.follow(currentUserService.get(principal), userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/follow/{userId}")
    public ResponseEntity<Void> unfollow(@PathVariable Long userId,
                                         @AuthenticationPrincipal UserDetails principal) {
        socialService.unfollow(currentUserService.get(principal), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/following")
    public ResponseEntity<List<UserSummaryDto>> following(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(socialService.getFollowing(currentUserService.get(principal)));
    }

    @GetMapping("/followers")
    public ResponseEntity<List<UserSummaryDto>> followers(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(socialService.getFollowers(currentUserService.get(principal)));
    }

    @GetMapping("/feed")
    public ResponseEntity<PageResponse<PostDto>> feed(
            @AuthenticationPrincipal UserDetails principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(
                postService.getFeed(currentUserService.get(principal), pageable)));
    }

    @GetMapping("/discovery")
    public ResponseEntity<PageResponse<PostDto>> discovery(
            @AuthenticationPrincipal UserDetails principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(
                postService.getDiscovery(currentUserService.get(principal), pageable)));
    }
}
