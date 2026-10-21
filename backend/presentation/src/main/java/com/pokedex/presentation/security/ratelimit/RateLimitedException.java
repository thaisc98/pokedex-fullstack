package com.pokedex.presentation.security.ratelimit;

/** Too many attempts: 429 {@code /problems/rate-limited} with {@code Retry-After} (security.md §4.1). */
public final class RateLimitedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long retryAfterSeconds;

    public RateLimitedException(long retryAfterSeconds) {
        super("rate limit exceeded");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
