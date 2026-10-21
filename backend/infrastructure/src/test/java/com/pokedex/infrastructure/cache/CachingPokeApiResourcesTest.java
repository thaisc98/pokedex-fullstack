package com.pokedex.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.pokedex.infrastructure.pokeapi.PokeApiResources;
import com.pokedex.infrastructure.pokeapi.dto.NamedResourceDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiEvolutionChainDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiListDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;

/**
 * ADR 0004: each PokeAPI resource is cached on its own ({@code /pokemon/{id}}, {@code /pokemon-species/{id}},
 * {@code /evolution-chain/{id}}), so the list fan-out and the detail share them. Failures (404 included) are never
 * cached.
 */
class CachingPokeApiResourcesTest {

    private final RecordingResources upstream = new RecordingResources();
    private final Cache pokemonCache = cache(CachingPokeApiResources.POKEMON_CACHE);
    private final Cache speciesCache = cache(CachingPokeApiResources.SPECIES_CACHE);
    private final Cache chainCache = cache(CachingPokeApiResources.EVOLUTION_CHAIN_CACHE);
    private final CachingPokeApiResources resources =
            new CachingPokeApiResources(upstream, pokemonCache, speciesCache, chainCache);

    @Test
    void should_fetch_a_pokemon_once_by_id() {
        PokeApiPokemonDto first = resources.pokemon(25);
        PokeApiPokemonDto second = resources.pokemon(25);

        assertThat(second).isSameAs(first);
        assertThat(upstream.calls).containsExactly("pokemon/25");
    }

    @Test
    void should_serve_a_later_lookup_by_id_from_a_lookup_by_name() {
        resources.pokemonByName("pikachu");

        resources.pokemon(25);
        resources.pokemonByName("pikachu");

        assertThat(upstream.calls).containsExactly("pokemon/pikachu");
    }

    @Test
    void should_fetch_species_and_chains_once_each() {
        resources.species(25);
        resources.species(25);
        resources.evolutionChain(10);
        resources.evolutionChain(10);
        resources.species(1);

        assertThat(upstream.calls).containsExactly("pokemon-species/25", "evolution-chain/10", "pokemon-species/1");
    }

    @Test
    void should_not_cache_the_list_call() {
        resources.list(0, 3);
        resources.list(0, 3);

        assertThat(upstream.calls).containsExactly("list", "list");
    }

    @Test
    void should_rethrow_a_failure_unchanged_and_not_cache_it() {
        IllegalStateException notFound = new IllegalStateException("404");
        upstream.pokemonAnswer = () -> {
            throw notFound;
        };

        assertThatThrownBy(() -> resources.pokemon(99999)).isSameAs(notFound);
        assertThatThrownBy(() -> resources.pokemon(99999)).isSameAs(notFound);

        assertThat(upstream.calls).containsExactly("pokemon/99999", "pokemon/99999");
        assertThat(pokemonCache.get(99999)).isNull();
    }

    @Test
    void should_rethrow_the_cache_wrapper_for_a_checked_failure() {
        IOException checked = new IOException("socket closed");
        upstream.pokemonAnswer = () -> sneakyThrow(checked);

        assertThatThrownBy(() -> resources.pokemon(1))
                .isInstanceOf(Cache.ValueRetrievalException.class)
                .hasCause(checked);
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T sneakyThrow(Throwable throwable) throws E {
        throw (E) throwable;
    }

    private static Cache cache(String name) {
        return new CaffeineCache(name, Caffeine.newBuilder().maximumSize(100).build());
    }

    /** Upstream stand-in that records calls and returns minimal DTOs. */
    private static final class RecordingResources implements PokeApiResources {

        final List<String> calls = new ArrayList<>();
        Supplier<PokeApiPokemonDto> pokemonAnswer = () -> pokemonDto(25, "pikachu");

        @Override
        public PokeApiListDto list(long offset, int limit) {
            calls.add("list");
            return new PokeApiListDto(0L, List.of());
        }

        @Override
        public PokeApiPokemonDto pokemon(int id) {
            calls.add("pokemon/" + id);
            return pokemonAnswer.get();
        }

        @Override
        public PokeApiPokemonDto pokemonByName(String name) {
            calls.add("pokemon/" + name);
            return pokemonAnswer.get();
        }

        @Override
        public PokeApiSpeciesDto species(int id) {
            calls.add("pokemon-species/" + id);
            return new PokeApiSpeciesDto(id, List.of(), List.of(), null);
        }

        @Override
        public PokeApiEvolutionChainDto evolutionChain(int id) {
            calls.add("evolution-chain/" + id);
            return new PokeApiEvolutionChainDto(id, null);
        }

        private static PokeApiPokemonDto pokemonDto(int id, String name) {
            return new PokeApiPokemonDto(
                    id, name, 60, 4, null, List.of(), List.of(), List.of(), new NamedResourceDto(name, null));
        }
    }
}
