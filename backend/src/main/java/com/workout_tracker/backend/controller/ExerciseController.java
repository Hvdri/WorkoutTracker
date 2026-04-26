package com.workout_tracker.backend.controller;

import com.workout_tracker.backend.dto.common.PageResponse;
import com.workout_tracker.backend.dto.workout.CreateExerciseRequest;
import com.workout_tracker.backend.dto.workout.ExerciseDto;
import com.workout_tracker.backend.model.enums.MuscleGroup;
import com.workout_tracker.backend.security.annotation.AdminOnly;
import com.workout_tracker.backend.service.ExerciseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    // GET /api/exercises?muscleGroup=CHEST&name=press&page=0&size=20&sort=name,asc
    // Both filters are optional and combine when both are present.
    @GetMapping
    public ResponseEntity<PageResponse<ExerciseDto>> list(
            @RequestParam(required = false) MuscleGroup muscleGroup,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.of(exerciseService.findExercises(muscleGroup, name, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExerciseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(exerciseService.getExerciseById(id));
    }

    @PostMapping
    @AdminOnly
    public ResponseEntity<ExerciseDto> create(@Valid @RequestBody CreateExerciseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exerciseService.createExercise(request));
    }

    @PutMapping("/{id}")
    @AdminOnly
    public ResponseEntity<ExerciseDto> update(@PathVariable Long id,
                                              @Valid @RequestBody CreateExerciseRequest request) {
        return ResponseEntity.ok(exerciseService.updateExercise(id, request));
    }

    @DeleteMapping("/{id}")
    @AdminOnly
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        exerciseService.deleteExercise(id);
        return ResponseEntity.noContent().build();
    }
}
