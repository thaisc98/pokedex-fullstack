package com.pokedex.domain.catalog.vo;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.Locale;
import java.util.Objects;

/** The closed set of 18 Pokémon types (openapi {@code PokemonType}, ui-ux.md §6 {@code bg-type-*}). */
public enum PokemonType {
    NORMAL,
    FIRE,
    WATER,
    ELECTRIC,
    GRASS,
    ICE,
    FIGHTING,
    POISON,
    GROUND,
    FLYING,
    PSYCHIC,
    BUG,
    ROCK,
    GHOST,
    DRAGON,
    DARK,
    STEEL,
    FAIRY;

    private final String slug = name().toLowerCase(Locale.ROOT);

    public String slug() {
        return slug;
    }

    public static PokemonType fromSlug(String slug) {
        Objects.requireNonNull(slug, "slug");
        for (PokemonType type : values()) {
            if (type.slug.equals(slug)) {
                return type;
            }
        }
        throw new InvalidValueException("pokemonType", "must be one of the 18 Pokémon types");
    }
}
