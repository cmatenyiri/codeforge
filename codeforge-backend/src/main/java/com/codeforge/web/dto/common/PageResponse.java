package com.codeforge.web.dto.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * A slice of results.
 *
 * <p>Deliberately hand-rolled rather than serializing Spring's {@code Page}: that
 * type's JSON shape is an implementation detail that has changed between
 * versions, and this keeps the contract ours.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
