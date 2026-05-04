package com.workout_tracker.backend.controller;

import com.workout_tracker.backend.dto.social.CreatePostRequest;
import com.workout_tracker.backend.dto.social.PostDto;
import com.workout_tracker.backend.service.CurrentUserService;
import com.workout_tracker.backend.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<PostDto> create(@Valid @RequestBody CreatePostRequest request,
                                          @AuthenticationPrincipal UserDetails principal) {
        PostDto created = postService.createPost(currentUserService.get(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails principal) {
        postService.deletePost(id, currentUserService.get(principal));
        return ResponseEntity.noContent().build();
    }
}
