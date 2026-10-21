package com.pokedex.domain.catalog.vo;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.Objects;

/** The six core statistics, in canonical order, with their PokeAPI slugs. */
public enum StatName {
    HP("hp"),
    ATTACK("attack"),
    DEFENSE("defense"),
    SPECIAL_ATTACK("special-attack"),
    SPECIAL_DEFENSE("special-defense"),
    SPEED("speed");

    private final String slug;

    StatName(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    public static StatName fromSlug(String slug) {
        Objects.requireNonNull(slug, "slug");
        for (StatName name : values()) {
            if (name.slug.equals(slug)) {
                return name;
            }
        }
        throw new InvalidValueException("stat", "must be one of the six core stats");
    }
}
