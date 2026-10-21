package com.pokedex.presentation.rest.shared;

import java.util.List;

/** openapi {@code PageResponse}: {@code {content, page, size, totalElements, totalPages}}. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public PageResponse {
        content = List.copyOf(content);
    }
}
