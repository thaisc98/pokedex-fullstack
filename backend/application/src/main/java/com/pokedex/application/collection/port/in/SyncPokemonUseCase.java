package com.pokedex.application.collection.port.in;

import com.pokedex.domain.catalog.vo.PokedexId;

/** UC-03: idempotent upsert of a Pokémon into the collection (BR-05). */
public interface SyncPokemonUseCase {

    SyncResult sync(PokedexId id);
}
