package com.pokedex.domain.catalog;

import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import java.util.Objects;

/** A Pokémon by id and name: one entry of the catalog's name index (search). */
public record PokemonNameRef(PokedexId id, PokemonName name) {

    public PokemonNameRef {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
