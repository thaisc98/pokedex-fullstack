package com.pokedex.infrastructure.cache;

import com.pokedex.infrastructure.pokeapi.PokeApiResources;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiEvolutionChainDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiListDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;
import java.util.Objects;
import org.springframework.cache.Cache;

/**
 * ADR 0004: caches each PokeAPI resource on its own, so the list fan-out and the detail share {@code
 * /pokemon/{id}}, {@code /pokemon-species/{id}} and {@code /evolution-chain/{id}}. A lookup by name is also stored
 * under the Pokémon's id. Failures, a PokeAPI 404 included, are never cached. The list call is not cached here
 * (pages are cached by {@link CachingPokemonCatalog}).
 */
public final class CachingPokeApiResources implements PokeApiResources {

    public static final String POKEMON_CACHE = "pokeapi-pokemon";
    public static final String SPECIES_CACHE = "pokeapi-species";
    public static final String EVOLUTION_CHAIN_CACHE = "pokeapi-evolution-chain";

    private final PokeApiResources delegate;
    private final Cache pokemon;
    private final Cache species;
    private final Cache evolutionChains;

    public CachingPokeApiResources(PokeApiResources delegate, Cache pokemon, Cache species, Cache evolutionChains) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.pokemon = Objects.requireNonNull(pokemon, "pokemon");
        this.species = Objects.requireNonNull(species, "species");
        this.evolutionChains = Objects.requireNonNull(evolutionChains, "evolutionChains");
    }

    @Override
    public PokeApiListDto list(long offset, int limit) {
        return delegate.list(offset, limit);
    }

    @Override
    public PokeApiPokemonDto pokemon(int id) {
        return CacheLookup.get(pokemon, id, () -> delegate.pokemon(id));
    }

    @Override
    public PokeApiPokemonDto pokemonByName(String name) {
        PokeApiPokemonDto found = CacheLookup.get(pokemon, nameKey(name), () -> delegate.pokemonByName(name));
        if (found.id() != null) {
            pokemon.putIfAbsent(found.id(), found);
        }
        return found;
    }

    @Override
    public PokeApiSpeciesDto species(int id) {
        return CacheLookup.get(species, id, () -> delegate.species(id));
    }

    @Override
    public PokeApiEvolutionChainDto evolutionChain(int id) {
        return CacheLookup.get(evolutionChains, id, () -> delegate.evolutionChain(id));
    }

    /** Names and ids share one cache; a prefixed string key can never collide with an integer id. */
    static String nameKey(String name) {
        return "name:" + name;
    }
}
