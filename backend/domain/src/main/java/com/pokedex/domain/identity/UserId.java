package com.pokedex.domain.identity;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** A user's id (UUID). */
public record UserId(UUID value) {

    private static final Pattern CANONICAL =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    public UserId {
        Objects.requireNonNull(value, "value");
    }

    public static UserId random() {
        return new UserId(UUID.randomUUID());
    }

    /** Parses the canonical 36-character form, e.g. the JWT {@code sub} claim. */
    public static UserId parse(String text) {
        Objects.requireNonNull(text, "text");
        if (!CANONICAL.matcher(text).matches()) {
            throw new InvalidValueException("userId", "must be a UUID");
        }
        return new UserId(UUID.fromString(text));
    }
}
