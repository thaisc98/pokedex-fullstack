package com.pokedex.domain.shared;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * One page of results. A page past the end has empty content and is not an error (BR-01).
 *
 * @param content the items on this page, never more than {@code request.size()}
 * @param request the page that was asked for
 * @param totalElements the number of items across all pages
 * @param <T> the item type
 */
public record Page<T>(List<T> content, PageRequest request, long totalElements) {

    public Page {
        Objects.requireNonNull(request, "request");
        content = List.copyOf(Objects.requireNonNull(content, "content"));
        if (totalElements < 0) {
            throw new InvalidValueException("totalElements", "must be greater than or equal to 0");
        }
        if (content.size() > request.size()) {
            throw new InvalidValueException("content", "must not contain more items than the page size");
        }
    }

    public int page() {
        return request.page();
    }

    public int size() {
        return request.size();
    }

    /** {@code ceil(totalElements / size)}. */
    public int totalPages() {
        return Math.toIntExact((totalElements + request.size() - 1) / request.size());
    }

    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new Page<>(mapped, request, totalElements);
    }
}
