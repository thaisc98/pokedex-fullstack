package com.pokedex.application.identity.port.out;

import com.pokedex.domain.identity.PasswordHash;

/** One-way password hashing (BCrypt behind a delegating encoder in production). */
public interface PasswordHasher {

    PasswordHash hash(String rawPassword);

    boolean matches(String rawPassword, PasswordHash hash);

    /**
     * A valid hash of a random secret, produced by the same encoder and cost as real hashes. Login compares against
     * it when the email is unknown, so timing does not reveal which emails exist.
     */
    PasswordHash dummyHash();
}
