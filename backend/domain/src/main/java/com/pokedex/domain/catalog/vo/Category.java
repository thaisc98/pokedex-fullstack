package com.pokedex.domain.catalog.vo;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Species genus such as {@code Mouse Pokémon}: non-blank, at most 100 characters, or {@link #UNKNOWN}. */
public record Category(String value) {

    public static final int MAX_LENGTH = 100;

    /** Used when the species has no English genus. */
    public static final Category UNKNOWN = new Category("Unknown");

    public Category {
        value = Objects.requireNonNull(value, "value").strip();
        if (value.isEmpty()) {
            throw new InvalidValueException("category", "must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidValueException("category", "must be at most " + MAX_LENGTH + " characters");
        }
    }

    /** The genus, or {@link #UNKNOWN} when it is missing or blank. */
    public static Category orUnknown(@Nullable String value) {
        return value == null || value.isBlank() ? UNKNOWN : new Category(value);
    }

    public boolean isUnknown() {
        return equals(UNKNOWN);
    }
}
