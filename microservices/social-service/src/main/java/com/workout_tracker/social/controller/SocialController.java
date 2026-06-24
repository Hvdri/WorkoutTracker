package com.workout_tracker.social.controller;

import com.workout_tracker.social.dto.PageResponse;
import com.workout_tracker.social.dto.PostDto;
import com.workout_tracker.social.dto.UserSummaryDto;
import com.workout_tracker.social.security.AuthenticatedUser;
import com.workout_tracker.social.service.PostService;
import com.workout_tracker.social.service.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;
    private final PostService postService;

    @PostMapping("/follow/{userId}")
    public ResponseEntity<Void> follow(@PathVariable Long userId,
                                       @AuthenticationPrincipal AuthenticatedUser current) {
        socialService.follow(current.userId(), current.username(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/follow/{userId}")
    public ResponseEntity<Void> unfollow(@PathVariable Long userId,
                                         @AuthenticationPrincipal AuthenticatedUser current) {
        socialService.unfollow(current.userId(), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/following")
    public ResponseEntity<List<UserSummaryDto>> following(
            @AuthenticationPrincipal AuthenticatedUser current) {
        return ResponseEntity.ok(socialService.getFollowing(current.userId()));
    }

    @GetMapping("/followers")
    public ResponseEntity<List<UserSummaryDto>> followers(
            @AuthenticationPrincipal AuthenticatedUser current) {
        return ResponseEntity.ok(socialService.getFollowers(current.userId()));
    }

    @GetMapping("/feed")
    public ResponseEntity<PageResponse<PostDto>> feed(
            @AuthenticationPrincipal AuthenticatedUser current,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(
                postService.getFeed(current.userId(), pageable)));
    }

    @GetMapping("/discovery")
    public ResponseEntity<PageResponse<PostDto>> discovery(
            @AuthenticationPrincipal AuthenticatedUser current,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(
                postService.getDiscovery(current.userId(), pageable)));
    }
}
