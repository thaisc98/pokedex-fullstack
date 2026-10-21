package com.pokedex.domain.identity;

import java.time.Instant;
import java.util.Objects;

/**
 * User aggregate root (architecture.md §1.2). Holds only the password hash (BR-09). {@link #toString()} is safe to
 * log because {@link PasswordHash} never prints its value.
 */
public record User(
        UserId id, Email email, DisplayName displayName, PasswordHash passwordHash, Role role, Instant createdAt) {

    public User {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** A new self-registered account: always {@link Role#USER}. */
    public static User register(
            UserId id, Email email, DisplayName displayName, PasswordHash passwordHash, Instant createdAt) {
        return new User(id, email, displayName, passwordHash, Role.USER, createdAt);
    }
}
