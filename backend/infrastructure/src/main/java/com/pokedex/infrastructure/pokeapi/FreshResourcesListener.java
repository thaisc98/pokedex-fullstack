package com.pokedex.infrastructure.pokeapi;

import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;

/**
 * Told about PokeAPI resources that were just read live (a sync), so the catalog caches can be refreshed with
 * them.
 */
@FunctionalInterface
public interface FreshResourcesListener {

    /** Does nothing. */
    FreshResourcesListener NONE = (pokemon, species) -> {};

    void refreshed(PokeApiPokemonDto pokemon, PokeApiSpeciesDto species);
}
