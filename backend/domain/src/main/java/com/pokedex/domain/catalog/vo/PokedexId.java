package com.pokedex.domain.catalog.vo;

import com.pokedex.domain.shared.InvalidValueException;

/** PokeAPI numeric id; always greater than 0. */
public record PokedexId(int value) {

    public PokedexId {
        if (value <= 0) {
            throw new InvalidValueException("pokedexId", "must be greater than 0");
        }
    }
}
