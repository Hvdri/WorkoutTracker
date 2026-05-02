package com.workout_tracker.backend.service;

import com.workout_tracker.backend.dto.workout.AddExerciseToTemplateRequest;
import com.workout_tracker.backend.dto.workout.CreateTemplateRequest;
import com.workout_tracker.backend.dto.workout.ExerciseTemplateDto;
import com.workout_tracker.backend.dto.workout.WorkoutTemplateDto;
import com.workout_tracker.backend.exception.ResourceNotFoundException;
import com.workout_tracker.backend.model.Exercise;
import com.workout_tracker.backend.model.ExerciseTemplate;
import com.workout_tracker.backend.model.User;
import com.workout_tracker.backend.model.WorkoutSplit;
import com.workout_tracker.backend.model.WorkoutTemplate;
import com.workout_tracker.backend.repository.ExerciseRepository;
import com.workout_tracker.backend.repository.ExerciseTemplateRepository;
import com.workout_tracker.backend.repository.WorkoutTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkoutTemplateService {

    private final WorkoutTemplateRepository templateRepository;
    private final WorkoutSplitService splitService;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseTemplateRepository exerciseTemplateRepository;

    @Transactional
    public WorkoutTemplateDto addTemplate(Long splitId, User user, CreateTemplateRequest request) {
        WorkoutSplit split = splitService.loadOwnedSplit(splitId, user);

        WorkoutTemplate template = WorkoutTemplate.builder()
                .name(request.name())
                .orderIndex(request.orderIndex())
                .split(split)
                .build();

        WorkoutTemplate saved = templateRepository.save(template);
        split.getTemplates().add(saved);  // keep parent consistent in same persistence context

        log.info("WorkoutTemplate {} added to split {} by user {}", saved.getId(), splitId, user.getUsername());
        return WorkoutMapper.toTemplateDto(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkoutTemplateDto> getTemplatesForSplit(Long splitId, User user) {
        WorkoutSplit split = splitService.loadOwnedSplit(splitId, user);
        return templateRepository.findBySplitOrderByOrderIndexAsc(split).stream()
                .map(WorkoutMapper::toTemplateDto)
                .toList();
    }

    @Transactional
    public void deleteTemplate(Long splitId, Long templateId, User user) {
        WorkoutTemplate template = loadOwnedTemplate(splitId, templateId, user);
        templateRepository.delete(template);
        log.info("WorkoutTemplate {} deleted by user {}", templateId, user.getUsername());
    }

    @Transactional
    public ExerciseTemplateDto addExerciseToTemplate(Long splitId, Long templateId, User user,
                                                     AddExerciseToTemplateRequest request) {
        WorkoutTemplate template = loadOwnedTemplate(splitId, templateId, user);

        Exercise exercise = exerciseRepository.findById(request.exerciseId())
                .orElseThrow(() -> ResourceNotFoundException.of("Exercise", request.exerciseId()));

        ExerciseTemplate et = ExerciseTemplate.builder()
                .template(template)
                .exercise(exercise)
                .targetSets(request.targetSets())
                .targetReps(request.targetReps())
                .orderIndex(request.orderIndex())
                .build();

        ExerciseTemplate saved = exerciseTemplateRepository.save(et);
        template.getExerciseTemplates().add(saved);  // keep parent consistent in same persistence context

        log.info("Exercise {} added to template {} by user {}",
                exercise.getId(), templateId, user.getUsername());
        return WorkoutMapper.toExerciseTemplateDto(saved);
    }

    @Transactional
    public void removeExerciseFromTemplate(Long splitId, Long templateId, Long exerciseTemplateId, User user) {
        ExerciseTemplate et = exerciseTemplateRepository.findById(exerciseTemplateId)
                .orElseThrow(() -> ResourceNotFoundException.of("ExerciseTemplate", exerciseTemplateId));

        // Walk parent chain — ownership AND URL-hierarchy assertion.
        // Both miss-and-wrong-owner cases surface as 404 (ProjectContext rule #2).
        if (!et.getTemplate().getSplit().getUser().getId().equals(user.getId())
                || !et.getTemplate().getId().equals(templateId)
                || !et.getTemplate().getSplit().getId().equals(splitId)) {
            log.warn("User {} attempted to remove ExerciseTemplate {} they do not own or wrong path",
                    user.getUsername(), exerciseTemplateId);
            throw ResourceNotFoundException.of("ExerciseTemplate", exerciseTemplateId);
        }

        exerciseTemplateRepository.delete(et);
        log.info("ExerciseTemplate {} removed by user {}", exerciseTemplateId, user.getUsername());
    }

    // Loads a template and confirms the user owns the split it belongs to.
    // Package-private so WorkoutLogService can reuse it for rule #2.
    // Both miss-and-wrong-owner cases surface as 404 (ProjectContext rule #2).
    WorkoutTemplate loadOwnedTemplate(Long templateId, User user) {
        WorkoutTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> ResourceNotFoundException.of("WorkoutTemplate", templateId));
        if (!template.getSplit().getUser().getId().equals(user.getId())) {
            log.warn("User {} attempted to access WorkoutTemplate {} they do not own",
                    user.getUsername(), templateId);
            throw ResourceNotFoundException.of("WorkoutTemplate", templateId);
        }
        return template;
    }

    // Same as loadOwnedTemplate but also asserts the template belongs to the splitId from the URL.
    // Used by routes nested under /api/splits/{splitId}/templates/{templateId}.
    WorkoutTemplate loadOwnedTemplate(Long splitId, Long templateId, User user) {
        WorkoutTemplate template = loadOwnedTemplate(templateId, user);
        if (!template.getSplit().getId().equals(splitId)) {
            log.warn("Template {} accessed via wrong splitId {} (actual {})",
                    templateId, splitId, template.getSplit().getId());
            throw ResourceNotFoundException.of("WorkoutTemplate", templateId);
        }
        return template;
    }
}
