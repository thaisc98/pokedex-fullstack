package com.pokedex.domain.shared;

/** BR-01: zero-based {@code page} ≥ 0 and 1 ≤ {@code size} ≤ 100. */
public record PageRequest(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageRequest {
        if (page < 0) {
            throw new InvalidValueException("page", "must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidValueException("size", "must be between 1 and " + MAX_SIZE);
        }
    }

    public static PageRequest ofPage(int page) {
        return new PageRequest(page, DEFAULT_SIZE);
    }

    /** Index of the first element on this page. A {@code long}, so a huge page never overflows. */
    public long offset() {
        return (long) page * size;
    }
}
