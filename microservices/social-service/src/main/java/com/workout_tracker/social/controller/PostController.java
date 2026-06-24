package com.workout_tracker.social.controller;

import com.workout_tracker.social.dto.CreatePostRequest;
import com.workout_tracker.social.dto.PageResponse;
import com.workout_tracker.social.dto.PostDto;
import com.workout_tracker.social.security.AuthenticatedUser;
import com.workout_tracker.social.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/api/posts")
    public ResponseEntity<PostDto> createPost(@Valid @RequestBody CreatePostRequest request,
                                              @AuthenticationPrincipal AuthenticatedUser current) {
        PostDto post = postService.createPost(current.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    @DeleteMapping("/api/posts/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id,
                                           @AuthenticationPrincipal AuthenticatedUser current) {
        postService.deletePost(id, current.userId());
        return ResponseEntity.noContent().build();
    }

    // Cross-cutting path: /api/users/{id}/posts. The matching route in the frontend's
    // Vite proxy routes this prefix to social-service, ahead of the catch-all /api → monolith.
    @GetMapping("/api/users/{userId}/posts")
    public ResponseEntity<PageResponse<PostDto>> getUserPosts(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(
                postService.getUserPosts(userId, pageable)));
    }
}
