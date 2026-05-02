package com.workout_tracker.backend.controller;

import com.workout_tracker.backend.dto.common.PageResponse;
import com.workout_tracker.backend.dto.workout.CreateWorkoutLogRequest;
import com.workout_tracker.backend.dto.workout.UpdateWorkoutLogRequest;
import com.workout_tracker.backend.dto.workout.WorkoutLogDto;
import com.workout_tracker.backend.service.CurrentUserService;
import com.workout_tracker.backend.service.WorkoutLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class WorkoutLogController {

    private final WorkoutLogService logService;
    private final CurrentUserService currentUserService;

    // Repo enforces date-desc as default; pageable.sort overrides if the client sends one.
    @GetMapping
    public ResponseEntity<PageResponse<WorkoutLogDto>> history(
            @AuthenticationPrincipal UserDetails principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.of(
                logService.getLogHistory(currentUserService.get(principal), pageable)));
    }

    @PostMapping
    public ResponseEntity<WorkoutLogDto> create(@Valid @RequestBody CreateWorkoutLogRequest request,
                                                @AuthenticationPrincipal UserDetails principal) {
        WorkoutLogDto created = logService.createLog(currentUserService.get(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkoutLogDto> getById(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(logService.getLogById(id, currentUserService.get(principal)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkoutLogDto> update(@PathVariable Long id,
                                                @Valid @RequestBody UpdateWorkoutLogRequest request,
                                                @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(logService.updateLog(id, currentUserService.get(principal), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails principal) {
        logService.deleteLog(id, currentUserService.get(principal));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<WorkoutLogDto> complete(@PathVariable Long id,
                                                  @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(logService.completeLog(id, currentUserService.get(principal)));
    }
}
