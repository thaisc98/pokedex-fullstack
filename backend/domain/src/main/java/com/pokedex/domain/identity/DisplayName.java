package com.pokedex.domain.identity;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.Objects;

/** A user's display name: trimmed, 1–60 code points, no control characters (architecture.md §1.2). */
public record DisplayName(String value) {

    public static final int MAX_LENGTH = 60;

    public DisplayName {
        Objects.requireNonNull(value, "value");
        value = value.strip();
        int length = value.codePointCount(0, value.length());
        if (length < 1 || length > MAX_LENGTH || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new InvalidValueException("displayName", "must be 1-60 characters without control characters");
        }
    }
}
