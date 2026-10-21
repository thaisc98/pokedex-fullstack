package com.pokedex.domain.catalog.vo;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Cleaned flavor text (BR-03): form feeds, line breaks and soft hyphens become spaces, every run of whitespace
 * (Unicode-aware, so no-break spaces too) becomes one space, and the ends are stripped. May be empty.
 */
public record Description(String value) {

    /**
     * Soft hyphen (U+00AD) plus any Unicode whitespace, which includes \f, \n, \r, \t and U+00A0. Declared before
     * {@link #EMPTY} because static fields initialize in order.
     */
    private static final Pattern SEPARATORS = Pattern.compile("[\\u00AD\\s]+", Pattern.UNICODE_CHARACTER_CLASS);

    public static final Description EMPTY = new Description("");

    public Description {
        value = SEPARATORS
                .matcher(Objects.requireNonNull(value, "value"))
                .replaceAll(" ")
                .strip();
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }
}
