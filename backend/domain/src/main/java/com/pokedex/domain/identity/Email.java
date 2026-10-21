package com.pokedex.domain.identity;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * An email address, trimmed and lowercased (BR-09, security.md §4.1). Accepts a pragmatic ASCII subset of
 * RFC 5321: a dot-atom local part of 1–64 characters and a domain of at least two DNS labels. At most 254
 * characters after normalization.
 */
public record Email(String value) {

    public static final int MAX_LENGTH = 254;
    private static final int MAX_LOCAL_PART = 64;

    private static final String ATOM = "[a-z0-9!#$%&'*+/=?^_`{|}~-]+";
    private static final String LABEL = "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?";
    private static final Pattern FORMAT =
            Pattern.compile("(" + ATOM + "(?:\\." + ATOM + ")*)@(" + LABEL + "(?:\\." + LABEL + ")+)");

    public Email {
        Objects.requireNonNull(value, "value");
        value = value.strip().toLowerCase(Locale.ROOT);
        var matcher = FORMAT.matcher(value);
        if (value.length() > MAX_LENGTH
                || !matcher.matches()
                || matcher.group(1).length() > MAX_LOCAL_PART) {
            throw new InvalidValueException("email", "must be a valid email address of at most 254 characters");
        }
    }
}
