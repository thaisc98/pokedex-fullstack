package com.pokedex.presentation.security.ratelimit;

import java.util.Objects;

/** security.md §4.1: 5 login attempts / minute per IP + email, 3 registrations / minute per IP. */
public record AuthRateLimits(RateLimiter login, RateLimiter registration) {

    /** Spring cache that holds every bucket (cleared by the ITs between tests). */
    public static final String CACHE = "rate-limit";

    public AuthRateLimits {
        Objects.requireNonNull(login, "login");
        Objects.requireNonNull(registration, "registration");
    }
}
