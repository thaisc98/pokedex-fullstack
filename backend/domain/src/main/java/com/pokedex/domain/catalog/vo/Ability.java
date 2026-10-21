package com.pokedex.domain.catalog.vo;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.Objects;

/** A Pokémon ability: PokeAPI slug name plus whether it is a hidden ability. */
public record Ability(String name, boolean hidden) {

    public Ability {
        Objects.requireNonNull(name, "name");
        if (!Slug.isValid(name)) {
            throw new InvalidValueException("ability", "must be 1-50 lowercase letters, digits or hyphens");
        }
    }
}
