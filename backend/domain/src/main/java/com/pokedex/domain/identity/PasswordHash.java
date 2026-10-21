package com.pokedex.domain.identity;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.Objects;

/** An opaque, encoder-prefixed password hash ({@code {bcrypt}…}). Never printed. */
public record PasswordHash(String value) {

    public PasswordHash {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new InvalidValueException("passwordHash", "must not be blank");
        }
    }

    @Override
    public String toString() {
        return "PasswordHash[***]";
    }
}
