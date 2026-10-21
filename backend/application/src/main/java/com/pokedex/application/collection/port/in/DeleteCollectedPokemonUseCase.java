package com.pokedex.application.collection.port.in;

import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.identity.Role;

/** UC-05: delete a collected Pokémon; ADMIN only (BR-10). */
public interface DeleteCollectedPokemonUseCase {

    void delete(PokedexId id, Role actorRole);
}
