package com.pokedex.infrastructure.security;

import com.pokedex.application.identity.port.out.PasswordHasher;
import com.pokedex.domain.identity.PasswordHash;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt (strength 12) behind a {@link DelegatingPasswordEncoder}, so hashes are stored as {@code {bcrypt}…} and the
 * algorithm can change later (security.md §4.1). The dummy hash for unknown-email logins is computed once, here,
 * with the same cost as a real hash.
 */
public final class BCryptPasswordHasher implements PasswordHasher {

    public static final int STRENGTH = 12;
    private static final String BCRYPT = "bcrypt";

    private final PasswordEncoder encoder;
    private final PasswordHash dummyHash;

    public BCryptPasswordHasher() {
        this(STRENGTH, new SecureRandom());
    }

    BCryptPasswordHasher(int strength, SecureRandom random) {
        this.encoder =
                new DelegatingPasswordEncoder(BCRYPT, Map.of(BCRYPT, new BCryptPasswordEncoder(strength, random)));
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        this.dummyHash = encode(Base64.getEncoder().encodeToString(secret));
    }

    @Override
    public PasswordHash hash(String rawPassword) {
        return encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
        try {
            return encoder.matches(rawPassword, hash.value());
        } catch (IllegalArgumentException e) {
            // unknown {id} prefix or a password BCrypt refuses (> 72 bytes): never a match, never a 500
            return false;
        }
    }

    @Override
    public PasswordHash dummyHash() {
        return dummyHash;
    }

    private PasswordHash encode(String rawPassword) {
        return new PasswordHash(Objects.requireNonNull(encoder.encode(rawPassword), "encoded password"));
    }
}
