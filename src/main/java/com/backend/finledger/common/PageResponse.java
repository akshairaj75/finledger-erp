package com.backend.finledger.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard paginated response format conforming to FinLedger REST conventions:
 * { content, page, size, totalElements, totalPages }
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> springPage) {
        return new PageResponse<>(
            springPage.getContent(),
            springPage.getNumber(),
            springPage.getSize(),
            springPage.getTotalElements(),
            springPage.getTotalPages()
        );
    }
}

