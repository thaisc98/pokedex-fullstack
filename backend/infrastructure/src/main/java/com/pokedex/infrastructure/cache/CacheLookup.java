package com.pokedex.infrastructure.cache;

import java.util.Objects;
import java.util.concurrent.Callable;
import org.springframework.cache.Cache;

/** Read-through on a Spring {@link Cache}: failures are not cached and runtime failures surface unchanged. */
final class CacheLookup {

    private CacheLookup() {}

    static <T> T get(Cache cache, Object key, Callable<T> loader) {
        try {
            return Objects.requireNonNull(cache.get(key, loader));
        } catch (Cache.ValueRetrievalException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }
}
