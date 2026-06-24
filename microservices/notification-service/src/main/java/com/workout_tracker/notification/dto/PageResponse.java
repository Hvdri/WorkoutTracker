package com.workout_tracker.notification.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// Same wire shape as monolith + social-service so frontend pagination types just work.
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
            page.getContent().stream().map(mapper).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}
