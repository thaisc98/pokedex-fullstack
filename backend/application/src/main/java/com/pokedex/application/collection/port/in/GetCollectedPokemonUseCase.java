package com.pokedex.application.collection.port.in;

import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.CollectedPokemon;

/** UC-05: one collected Pokémon by Pokédex id. */
public interface GetCollectedPokemonUseCase {

    CollectedPokemon get(PokedexId id);
}
