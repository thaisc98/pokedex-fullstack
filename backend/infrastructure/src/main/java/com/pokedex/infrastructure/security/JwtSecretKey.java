package com.pokedex.infrastructure.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;

/** security.md §4.2: the HS256 key is the raw UTF-8 bytes of {@code JWT_SECRET}, at least 256 bits. */
public final class JwtSecretKey {

    public static final int MIN_BYTES = 32;

    private JwtSecretKey() {}

    /** @throws IllegalStateException when the secret is missing or too short, so the app refuses to start */
    public static SecretKey fromSecret(@Nullable String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not set; it must be at least 256 bits (32 bytes)");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_BYTES) {
            throw new IllegalStateException("JWT_SECRET is too short; it must be at least 256 bits (32 bytes)");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
