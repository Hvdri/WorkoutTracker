package com.workout_tracker.backend.controller;

import com.workout_tracker.backend.dto.workout.CreateSetLogRequest;
import com.workout_tracker.backend.dto.workout.SetLogDto;
import com.workout_tracker.backend.dto.workout.UpdateSetLogRequest;
import com.workout_tracker.backend.service.CurrentUserService;
import com.workout_tracker.backend.service.SetLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs/{logId}/exercises/{exerciseLogId}/sets")
@RequiredArgsConstructor
public class SetLogController {

    private final SetLogService setLogService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<SetLogDto> create(@PathVariable Long logId,
                                            @PathVariable Long exerciseLogId,
                                            @Valid @RequestBody CreateSetLogRequest request,
                                            @AuthenticationPrincipal UserDetails principal) {
        SetLogDto created = setLogService.addSet(
                logId, exerciseLogId, currentUserService.get(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{setId}")
    public ResponseEntity<SetLogDto> update(@PathVariable Long logId,
                                            @PathVariable Long exerciseLogId,
                                            @PathVariable Long setId,
                                            @Valid @RequestBody UpdateSetLogRequest request,
                                            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(setLogService.updateSet(
                logId, exerciseLogId, setId, currentUserService.get(principal), request));
    }

    @DeleteMapping("/{setId}")
    public ResponseEntity<Void> delete(@PathVariable Long logId,
                                       @PathVariable Long exerciseLogId,
                                       @PathVariable Long setId,
                                       @AuthenticationPrincipal UserDetails principal) {
        setLogService.deleteSet(logId, exerciseLogId, setId, currentUserService.get(principal));
        return ResponseEntity.noContent().build();
    }
}
