package com.pokedex.infrastructure.cache;

import com.pokedex.application.catalog.port.out.PokemonCatalogPort;
import com.pokedex.domain.catalog.PokemonDetail;
import com.pokedex.domain.catalog.PokemonNameRef;
import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.catalog.vo.PokemonRef;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import java.util.List;
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

    /** Name of the search name-index cache inside the {@code CacheManager} (one entry, 24 h). */
    public static final String NAME_INDEX_CACHE = "pokemon-name-index";

    private static final String NAME_INDEX_KEY = "all";

    private final PokemonCatalogPort delegate;
    private final Cache pages;
    private final Cache nameIndex;

    public CachingPokemonCatalog(PokemonCatalogPort delegate, Cache pages, Cache nameIndex) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.pages = Objects.requireNonNull(pages, "pages");
        this.nameIndex = Objects.requireNonNull(nameIndex, "nameIndex");
    }

    /** The whole name index is one cache entry; a failed load is not cached. */
    @Override
    public List<PokemonNameRef> fetchNameIndex() {
        return CacheLookup.get(nameIndex, NAME_INDEX_KEY, delegate::fetchNameIndex);
    }

    @Override
    public Page<PokemonSummary> fetchPage(PageRequest request) {
        return CacheLookup.get(pages, request, () -> delegate.fetchPage(request));
    }

    /** Not cached as a whole: its PokeAPI resources are cached one by one ({@link CachingPokeApiResources}). */
    @Override
    public PokemonDetail fetchDetail(PokemonRef ref) {
        return delegate.fetchDetail(ref);
    }
}
