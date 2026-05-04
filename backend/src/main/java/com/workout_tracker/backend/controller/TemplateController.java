package com.workout_tracker.backend.controller;

import com.workout_tracker.backend.dto.workout.AddExerciseToTemplateRequest;
import com.workout_tracker.backend.dto.workout.CreateTemplateRequest;
import com.workout_tracker.backend.dto.workout.ExerciseTemplateDto;
import com.workout_tracker.backend.dto.workout.WorkoutTemplateDto;
import com.workout_tracker.backend.service.CurrentUserService;
import com.workout_tracker.backend.service.WorkoutTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/splits/{splitId}/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final WorkoutTemplateService templateService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<List<WorkoutTemplateDto>> list(@PathVariable Long splitId,
                                                         @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                templateService.getTemplatesForSplit(splitId, currentUserService.get(principal)));
    }

    @PostMapping
    public ResponseEntity<WorkoutTemplateDto> create(@PathVariable Long splitId,
                                                     @Valid @RequestBody CreateTemplateRequest request,
                                                     @AuthenticationPrincipal UserDetails principal) {
        WorkoutTemplateDto created = templateService.addTemplate(
                splitId, currentUserService.get(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // The service asserts the template's parent split matches splitId from the URL.
    // Mismatched paths surface as 404 — same as missing or wrong-owner.
    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> delete(@PathVariable Long splitId,
                                       @PathVariable Long templateId,
                                       @AuthenticationPrincipal UserDetails principal) {
        templateService.deleteTemplate(splitId, templateId, currentUserService.get(principal));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{templateId}/exercises")
    public ResponseEntity<ExerciseTemplateDto> addExercise(@PathVariable Long splitId,
                                                           @PathVariable Long templateId,
                                                           @Valid @RequestBody AddExerciseToTemplateRequest request,
                                                           @AuthenticationPrincipal UserDetails principal) {
        ExerciseTemplateDto created = templateService.addExerciseToTemplate(
                splitId, templateId, currentUserService.get(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{templateId}/exercises/{exerciseTemplateId}")
    public ResponseEntity<Void> removeExercise(@PathVariable Long splitId,
                                               @PathVariable Long templateId,
                                               @PathVariable Long exerciseTemplateId,
                                               @AuthenticationPrincipal UserDetails principal) {
        templateService.removeExerciseFromTemplate(splitId, templateId, exerciseTemplateId,
                currentUserService.get(principal));
        return ResponseEntity.noContent().build();
    }
}
