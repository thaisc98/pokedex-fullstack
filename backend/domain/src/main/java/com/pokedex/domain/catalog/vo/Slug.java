package com.pokedex.domain.catalog.vo;

import java.util.regex.Pattern;

/** The PokeAPI slug rule shared by names: lowercase letters, digits and hyphens, 1–50 characters. */
final class Slug {

    private static final Pattern PATTERN = Pattern.compile("[a-z0-9-]{1,50}");

    private Slug() {}

    /** Whole-value match, so a trailing newline or any other extra character fails. */
    static boolean isValid(String value) {
        return PATTERN.matcher(value).matches();
    }
}
