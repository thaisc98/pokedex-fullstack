package com.pokedex.presentation.security;

import java.time.Duration;
import java.util.Objects;
import org.springframework.http.ResponseCookie;

/**
 * The {@code access_token} cookie (security.md §4.2, ADR 0008): {@code HttpOnly; Secure; SameSite=Strict;
 * Path=/api; Max-Age=<token lifetime>}. JavaScript can never read it.
 */
public final class AccessTokenCookies {

    public static final String NAME = "access_token";
    static final String PATH = "/api";

    private final Duration lifetime;

    public AccessTokenCookies(Duration lifetime) {
        this.lifetime = Objects.requireNonNull(lifetime, "lifetime");
    }

    public ResponseCookie issue(String token) {
        return base(token).maxAge(lifetime).build();
    }

    /** Same name, path and attributes, empty value, {@code Max-Age=0}. */
    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private static ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(PATH);
    }
}
