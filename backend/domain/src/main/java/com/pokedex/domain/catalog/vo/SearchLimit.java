package com.pokedex.domain.catalog.vo;

import com.pokedex.domain.shared.InvalidValueException;

/** Maximum number of name-search results: 1–20, default 10. A violation is a 400 on field {@code limit}. */
public record SearchLimit(int value) {

    public static final int MAX = 20;
    public static final int DEFAULT_VALUE = 10;
    public static final SearchLimit DEFAULT = new SearchLimit(DEFAULT_VALUE);

    public SearchLimit {
        if (value < 1 || value > MAX) {
            throw new InvalidValueException("limit", "must be between 1 and " + MAX);
        }
    }
}
