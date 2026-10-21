package com.pokedex.presentation.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCache;

/**
 * security.md §4.1 brute-force protection: Bucket4j buckets kept in a Spring cache ({@code rate-limit}), so they are
 * bounded and evicted by the cache, and clearing the cache resets them. Fixed windows: N attempts per period, then
 * 429 with the seconds until the next refill.
 */
class RateLimiterTest {

    private final AtomicLong nanos = new AtomicLong();
    private final TimeMeter time = new TimeMeter() {
        @Override
        public long currentTimeNanos() {
            return nanos.get();
        }

        @Override
        public boolean isWallClockBased() {
            return false;
        }
    };
    private final ConcurrentMapCache cache = new ConcurrentMapCache("rate-limit");
    private final RateLimiter limiter = new RateLimiter(cache, "login", 5, Duration.ofMinutes(1), time);

    @Test
    void should_allow_the_capacity_then_reject_with_retry_after_seconds() {
        for (int i = 0; i < 5; i++) {
            limiter.consume("1.2.3.4|ash@example.com");
        }
        nanos.addAndGet(Duration.ofSeconds(15).toNanos());

        assertThatThrownBy(() -> limiter.consume("1.2.3.4|ash@example.com"))
                .isInstanceOfSatisfying(RateLimitedException.class, e -> assertThat(e.retryAfterSeconds())
                        .isEqualTo(45));
    }

    @Test
    void should_round_retry_after_up_to_at_least_one_second() {
        for (int i = 0; i < 5; i++) {
            limiter.consume("k");
        }
        nanos.addAndGet(Duration.ofSeconds(59).plusMillis(900).toNanos());

        assertThatThrownBy(() -> limiter.consume("k"))
                .isInstanceOfSatisfying(RateLimitedException.class, e -> assertThat(e.retryAfterSeconds())
                        .isEqualTo(1));
    }

    @Test
    void should_keep_separate_buckets_per_key() {
        for (int i = 0; i < 5; i++) {
            limiter.consume("1.2.3.4|ash@example.com");
        }

        assertThatCode(() -> limiter.consume("1.2.3.4|misty@example.com")).doesNotThrowAnyException();
    }

    @Test
    void should_refill_after_the_period() {
        for (int i = 0; i < 5; i++) {
            limiter.consume("k");
        }
        nanos.addAndGet(Duration.ofMinutes(1).toNanos());

        assertThatCode(() -> limiter.consume("k")).doesNotThrowAnyException();
    }

    @Test
    void should_reset_when_the_cache_is_cleared() {
        for (int i = 0; i < 5; i++) {
            limiter.consume("k");
        }
        cache.clear();

        assertThatCode(() -> limiter.consume("k")).doesNotThrowAnyException();
    }

    @Test
    void should_not_share_buckets_between_limiters_on_the_same_cache() {
        RateLimiter registration = new RateLimiter(cache, "register", 3, Duration.ofMinutes(1), time);
        for (int i = 0; i < 5; i++) {
            limiter.consume("1.2.3.4");
        }

        assertThatCode(() -> registration.consume("1.2.3.4")).doesNotThrowAnyException();
    }

    @Test
    void should_reject_a_non_positive_capacity() {
        assertThatThrownBy(() -> new RateLimiter(cache, "x", 0, Duration.ofMinutes(1), time))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
