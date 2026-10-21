package com.pokedex.domain.identity;

import com.pokedex.domain.shared.InvalidValueException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * BR-09 password policy (NIST 800-63B / ASVS): 12–64 code points and at most 72 UTF-8 bytes (BCrypt ignores
 * anything after byte 72), any Unicode, no composition rules, not on the common-password list. The list is
 * injected so the domain does no I/O; it is asked with the password lowercased ({@link Locale#ROOT}).
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 64;
    public static final int MAX_BYTES = 72;

    private static final String FIELD = "password";

    private final Predicate<String> isCommon;

    public PasswordPolicy(Predicate<String> isCommon) {
        this.isCommon = Objects.requireNonNull(isCommon, "isCommon");
    }

    /** Throws {@link InvalidValueException} on field {@code password}; the message never contains it. */
    public void check(String rawPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword");
        int length = rawPassword.codePointCount(0, rawPassword.length());
        if (length < MIN_LENGTH) {
            throw new InvalidValueException(FIELD, "must be at least 12 characters");
        }
        if (length > MAX_LENGTH || rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new InvalidValueException(FIELD, "must be at most 64 characters (72 bytes)");
        }
        if (isCommon.test(rawPassword.toLowerCase(Locale.ROOT))) {
            throw new InvalidValueException(FIELD, "is too common; choose a less common password");
        }
    }
}
