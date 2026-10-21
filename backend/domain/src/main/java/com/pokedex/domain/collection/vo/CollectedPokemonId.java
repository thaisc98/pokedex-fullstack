package com.pokedex.domain.collection.vo;

import java.util.Objects;
import java.util.UUID;

/** Internal identity of a collected Pokémon. Never exposed by the API (security.md §4.3). */
public record CollectedPokemonId(UUID value) {

    public CollectedPokemonId {
        Objects.requireNonNull(value, "value");
    }

    public static CollectedPokemonId random() {
        return new CollectedPokemonId(UUID.randomUUID());
    }
}
