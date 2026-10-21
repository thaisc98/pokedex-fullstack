package com.pokedex.application.collection.port.in;

import com.pokedex.application.collection.command.CollectionQuery;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.shared.Page;

/** UC-05: one page of the shared collection, optionally filtered. */
public interface ListCollectedPokemonUseCase {

    Page<CollectedPokemon> list(CollectionQuery query);
}
