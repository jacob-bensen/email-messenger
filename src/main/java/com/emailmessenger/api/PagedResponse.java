package com.emailmessenger.api;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Standard paginated envelope returned by every {@code /api/v1/**} list
 * endpoint. See {@code docs/API.md} §Pagination — the shape here is
 * part of the wire contract.
 */
record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    static <S, T> PagedResponse<T> of(Page<S> src, Function<S, T> mapper) {
        return new PagedResponse<>(
                src.getContent().stream().map(mapper).toList(),
                src.getNumber(),
                src.getSize(),
                src.getTotalElements(),
                src.getTotalPages(),
                src.hasNext());
    }
}
