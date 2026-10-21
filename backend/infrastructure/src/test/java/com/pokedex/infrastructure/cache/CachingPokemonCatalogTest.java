package com.pokedex.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.pokedex.application.catalog.StubPokemonCatalog;
import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;

/** ADR 0004: the cache decorator around {@code PokemonCatalogPort}. */
class CachingPokemonCatalogTest {

    private final StubPokemonCatalog delegate = new StubPokemonCatalog(List.of(summary(1), summary(2), summary(3)));
    private final Cache cache = new CaffeineCache(
            CachingPokemonCatalog.PAGES_CACHE,
            Caffeine.newBuilder().maximumSize(10).build());
    private final CachingPokemonCatalog catalog = new CachingPokemonCatalog(delegate, cache);

    @Test
    void should_serve_a_repeated_page_from_the_cache() {
        Page<PokemonSummary> first = catalog.fetchPage(new PageRequest(0, 2));
        Page<PokemonSummary> second = catalog.fetchPage(new PageRequest(0, 2));

        assertThat(second).isEqualTo(first);
        assertThat(delegate.requests()).containsExactly(new PageRequest(0, 2));
    }

    @Test
    void should_cache_each_page_separately() {
        catalog.fetchPage(new PageRequest(0, 2));
        catalog.fetchPage(new PageRequest(1, 2));
        catalog.fetchPage(new PageRequest(0, 3));

        assertThat(delegate.requests())
                .containsExactly(new PageRequest(0, 2), new PageRequest(1, 2), new PageRequest(0, 3));
    }

    @Test
    void should_fetch_again_after_the_cache_is_cleared() {
        catalog.fetchPage(new PageRequest(0, 2));
        cache.clear();
        catalog.fetchPage(new PageRequest(0, 2));

        assertThat(delegate.requests()).hasSize(2);
    }

    @Test
    void should_rethrow_the_original_failure_and_not_cache_it() {
        UpstreamUnavailableException down = new UpstreamUnavailableException("down", new IOException("reset"));
        StubPokemonCatalog failing = new StubPokemonCatalog(List.of()).failingWith(down);
        CachingPokemonCatalog failingCatalog = new CachingPokemonCatalog(failing, cache);

        assertThatThrownBy(() -> failingCatalog.fetchPage(PageRequest.ofPage(0)))
                .isSameAs(down);
        assertThatThrownBy(() -> failingCatalog.fetchPage(PageRequest.ofPage(0)))
                .isSameAs(down);

        assertThat(failing.requests()).hasSize(2);
        assertThat(cache.get(PageRequest.ofPage(0))).isNull();
    }

    private static PokemonSummary summary(int id) {
        return new PokemonSummary(
                new PokedexId(id),
                new PokemonName("pokemon-" + id),
                ImageUrl.PLACEHOLDER,
                Category.UNKNOWN,
                new Mass(id),
                List.of(new Ability("static", false)));
    }
}
