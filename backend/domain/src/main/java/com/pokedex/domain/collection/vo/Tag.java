package com.pokedex.domain.collection.vo;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** A user tag, normalized on creation (BR-07): stripped, lowercased, then {@code ^[a-z0-9-]{1,30}$}. */
public record Tag(String value) {

    private static final Pattern PATTERN = Pattern.compile("[a-z0-9-]{1,30}");

    public Tag {
        value = Objects.requireNonNull(value, "value").strip().toLowerCase(Locale.ROOT);
        if (!PATTERN.matcher(value).matches()) {
            throw new InvalidValueException("tag", "must be 1-30 lowercase letters, digits or hyphens");
        }
    }
}
