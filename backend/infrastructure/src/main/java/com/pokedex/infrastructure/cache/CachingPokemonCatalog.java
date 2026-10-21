package com.pokedex.infrastructure.cache;

import com.pokedex.application.catalog.port.out.PokemonCatalogPort;
import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import java.util.Objects;
import org.springframework.cache.Cache;

/**
 * ADR 0004: cache decorator around a {@link PokemonCatalogPort}. Complete pages are cached by their {@link
 * PageRequest}; failures are never cached and are rethrown unchanged. The {@link Cache} comes from a Spring
 * {@code CacheManager} (Caffeine, size-limited, TTL), so it can be inspected and cleared like any other cache.
 */
public final class CachingPokemonCatalog implements PokemonCatalogPort {

    /** Name of the page cache inside the {@code CacheManager}. */
    public static final String PAGES_CACHE = "pokemon-pages";

    private final PokemonCatalogPort delegate;
    private final Cache pages;

    public CachingPokemonCatalog(PokemonCatalogPort delegate, Cache pages) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.pages = Objects.requireNonNull(pages, "pages");
    }

    @Override
    public Page<PokemonSummary> fetchPage(PageRequest request) {
        try {
            return Objects.requireNonNull(pages.get(request, () -> delegate.fetchPage(request)));
        } catch (Cache.ValueRetrievalException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }
}
