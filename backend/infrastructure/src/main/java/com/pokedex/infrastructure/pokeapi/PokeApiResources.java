package com.pokedex.infrastructure.pokeapi;

import com.pokedex.infrastructure.pokeapi.dto.PokeApiEvolutionChainDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiListDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;

/**
 * The PokeAPI resources the catalog adapter reads. Implemented by the resilient {@link PokeApiGateway} and, in
 * front of it, by the per-resource cache ({@code CachingPokeApiResources}, ADR 0004).
 */
public interface PokeApiResources {

    PokeApiListDto list(long offset, int limit);

    PokeApiPokemonDto pokemon(int id);

    /** {@code name} has passed {@code PokemonName} validation. */
    PokeApiPokemonDto pokemonByName(String name);

    PokeApiSpeciesDto species(int id);

    PokeApiEvolutionChainDto evolutionChain(int id);
}
