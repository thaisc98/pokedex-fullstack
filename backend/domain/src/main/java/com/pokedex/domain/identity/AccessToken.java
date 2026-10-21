package com.pokedex.domain.identity;

import com.pokedex.domain.shared.InvalidValueException;
import java.time.Instant;
import java.util.Objects;

/** UC-07 output: an opaque signed token and its expiry. The value is never printed. */
public record AccessToken(String value, Instant expiresAt) {

    public AccessToken {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (value.isBlank()) {
            throw new InvalidValueException("accessToken", "must not be blank");
        }
    }

    @Override
    public String toString() {
        return "AccessToken[value=***, expiresAt=" + expiresAt + "]";
    }
}
