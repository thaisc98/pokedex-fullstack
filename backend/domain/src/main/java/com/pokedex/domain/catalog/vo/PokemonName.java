package com.pokedex.domain.catalog.vo;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.Objects;

/**
 * Lowercase PokeAPI name matching {@code ^[a-z0-9-]{1,50}$} ({@code mr-mime}, {@code ho-oh}). Input that
 * does not match is rejected, not normalized: lowercasing user input is the API layer's job.
 */
public record PokemonName(String value) {

    public PokemonName {
        Objects.requireNonNull(value, "value");
        if (!Slug.isValid(value)) {
            throw new InvalidValueException("pokemonName", "must be 1-50 lowercase letters, digits or hyphens");
        }
    }
}
