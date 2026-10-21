package com.pokedex.infrastructure.persistence;

/** Builds a "contains" {@code LIKE} pattern in which user text is literal (security.md §4.4). */
final class LikePattern {

    static final char ESCAPE = '\\';

    private LikePattern() {}

    /** {@code %text%} with {@code \}, {@code %} and {@code _} escaped by {@link #ESCAPE}. */
    static String contains(String text) {
        StringBuilder pattern = new StringBuilder(text.length() + 8).append('%');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ESCAPE || c == '%' || c == '_') {
                pattern.append(ESCAPE);
            }
            pattern.append(c);
        }
        return pattern.append('%').toString();
    }
}
