package com.pokedex.presentation.security.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.Objects;
import org.springframework.cache.Cache;

/**
 * Fixed-window limit of {@code capacity} attempts per {@code period} per key, with Bucket4j buckets kept in a Spring
 * {@link Cache} (the in-memory, size-bounded {@code rate-limit} cache), so idle buckets are evicted and clearing the
 * cache resets every limit. Keys are namespaced by the limiter name, so several limiters can share one cache.
 */
public final class RateLimiter {

    private static final long NANOS_PER_SECOND = Duration.ofSeconds(1).toNanos();

    private final Cache buckets;
    private final String name;
    private final long capacity;
    private final Duration period;
    private final TimeMeter time;

    public RateLimiter(Cache buckets, String name, long capacity, Duration period) {
        this(buckets, name, capacity, period, TimeMeter.SYSTEM_NANOTIME);
    }

    RateLimiter(Cache buckets, String name, long capacity, Duration period, TimeMeter time) {
        this.buckets = Objects.requireNonNull(buckets, "buckets");
        this.name = Objects.requireNonNull(name, "name");
        this.period = Objects.requireNonNull(period, "period");
        this.time = Objects.requireNonNull(time, "time");
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    /** Takes one attempt for {@code key}, or throws {@link RateLimitedException} with the wait in whole seconds. */
    public void consume(String key) {
        Bucket bucket = Objects.requireNonNull(buckets.get(name + ":" + key, this::newBucket), "bucket");
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long seconds = (probe.getNanosToWaitForRefill() + NANOS_PER_SECOND - 1) / NANOS_PER_SECOND;
            throw new RateLimitedException(Math.max(1, seconds));
        }
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .withCustomTimePrecision(time)
                .addLimit(limit -> limit.capacity(capacity).refillIntervally(capacity, period))
                .build();
    }
}
