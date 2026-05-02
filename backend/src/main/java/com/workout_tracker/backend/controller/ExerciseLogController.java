package com.workout_tracker.backend.controller;

import com.workout_tracker.backend.dto.workout.CreateExerciseLogRequest;
import com.workout_tracker.backend.dto.workout.ExerciseLogDto;
import com.workout_tracker.backend.service.CurrentUserService;
import com.workout_tracker.backend.service.ExerciseLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs/{logId}/exercises")
@RequiredArgsConstructor
public class ExerciseLogController {

    private final ExerciseLogService exerciseLogService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<List<ExerciseLogDto>> list(@PathVariable Long logId,
                                                     @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                exerciseLogService.getExerciseLogs(logId, currentUserService.get(principal)));
    }

    @PostMapping
    public ResponseEntity<ExerciseLogDto> create(@PathVariable Long logId,
                                                 @Valid @RequestBody CreateExerciseLogRequest request,
                                                 @AuthenticationPrincipal UserDetails principal) {
        ExerciseLogDto created = exerciseLogService.addExerciseLog(
                logId, currentUserService.get(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{exerciseLogId}")
    public ResponseEntity<Void> delete(@PathVariable Long logId,
                                       @PathVariable Long exerciseLogId,
                                       @AuthenticationPrincipal UserDetails principal) {
        exerciseLogService.deleteExerciseLog(logId, exerciseLogId, currentUserService.get(principal));
        return ResponseEntity.noContent().build();
    }
}
