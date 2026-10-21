package com.pokedex.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.infrastructure.pokeapi.dto.NamedResourceDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;

/**
 * A live re-sync refreshes the catalog (approved decision): the fresh {@code /pokemon/{id}} (also under its name
 * key) and {@code /pokemon-species/{id}} replace the cached ones, and the page cache is cleared, so the catalog
 * serves what the sync just stored.
 */
class CatalogCacheRefresherTest {

    private final Cache pokemonCache = cache(CachingPokeApiResources.POKEMON_CACHE);
    private final Cache speciesCache = cache(CachingPokeApiResources.SPECIES_CACHE);
    private final Cache pageCache = cache(CachingPokemonCatalog.PAGES_CACHE);
    private final CatalogCacheRefresher refresher = new CatalogCacheRefresher(pokemonCache, speciesCache, pageCache);

    @Test
    void should_replace_the_cached_pokemon_by_id_and_by_name() {
        pokemonCache.put(25, pokemon(25, 60));
        pokemonCache.put(CachingPokeApiResources.nameKey("pikachu"), pokemon(25, 60));
        PokeApiPokemonDto fresh = pokemon(25, 75);

        refresher.refreshed(fresh, species(25));

        assertThat(pokemonCache.get(25, PokeApiPokemonDto.class)).isSameAs(fresh);
        assertThat(pokemonCache.get(CachingPokeApiResources.nameKey("pikachu"), PokeApiPokemonDto.class))
                .isSameAs(fresh);
    }

    @Test
    void should_replace_the_cached_species() {
        speciesCache.put(25, species(25));
        PokeApiSpeciesDto fresh = species(25);

        refresher.refreshed(pokemon(25, 75), fresh);

        assertThat(speciesCache.get(25, PokeApiSpeciesDto.class)).isSameAs(fresh);
    }

    @Test
    void should_clear_every_cached_page() {
        pageCache.put(PageRequest.ofPage(0), "page 0");
        pageCache.put(new PageRequest(3, 7), "page 3");

        refresher.refreshed(pokemon(25, 75), species(25));

        assertThat(pageCache.get(PageRequest.ofPage(0))).isNull();
        assertThat(pageCache.get(new PageRequest(3, 7))).isNull();
    }

    @Test
    void should_skip_keys_it_cannot_know_without_failing() {
        PokeApiPokemonDto withoutIdOrName = new PokeApiPokemonDto(
                null, null, 75, 4, null, List.of(), List.of(), List.of(), new NamedResourceDto("pikachu", null));

        refresher.refreshed(withoutIdOrName, new PokeApiSpeciesDto(null, List.of(), List.of(), null));

        assertThat(pokemonCache.get(25)).isNull();
    }

    private static PokeApiPokemonDto pokemon(int id, int weight) {
        return new PokeApiPokemonDto(
                id, "pikachu", weight, 4, null, List.of(), List.of(), List.of(), new NamedResourceDto("pikachu", null));
    }

    private static PokeApiSpeciesDto species(int id) {
        return new PokeApiSpeciesDto(id, List.of(), List.of(), null);
    }

    private static Cache cache(String name) {
        return new CaffeineCache(name, Caffeine.newBuilder().maximumSize(100).build());
    }
}
