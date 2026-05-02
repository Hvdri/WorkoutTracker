package com.workout_tracker.backend.controller;

import com.workout_tracker.backend.dto.workout.CreateSplitRequest;
import com.workout_tracker.backend.dto.workout.WorkoutSplitDto;
import com.workout_tracker.backend.service.CurrentUserService;
import com.workout_tracker.backend.service.WorkoutSplitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/splits")
@RequiredArgsConstructor
public class SplitController {

    private final WorkoutSplitService splitService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<List<WorkoutSplitDto>> list(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(splitService.getAllSplits(currentUserService.get(principal)));
    }

    @GetMapping("/active")
    public ResponseEntity<WorkoutSplitDto> active(@AuthenticationPrincipal UserDetails principal) {
        return splitService.getActiveSplit(currentUserService.get(principal))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<WorkoutSplitDto> create(@Valid @RequestBody CreateSplitRequest request,
                                                  @AuthenticationPrincipal UserDetails principal) {
        WorkoutSplitDto created = splitService.createSplit(currentUserService.get(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkoutSplitDto> getById(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(splitService.getSplitById(id, currentUserService.get(principal)));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<WorkoutSplitDto> activate(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(splitService.activateSplit(id, currentUserService.get(principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails principal) {
        splitService.deleteSplit(id, currentUserService.get(principal));
        return ResponseEntity.noContent().build();
    }
}
