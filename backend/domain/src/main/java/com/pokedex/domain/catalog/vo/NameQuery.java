package com.pokedex.domain.catalog.vo;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.Locale;
import java.util.Objects;

/**
 * The normalized text of a Pokémon name search: trimmed, lowercased, spaces → hyphens ({@code Mr Mime} → {@code
 * mr-mime}), then {@code ^[a-z0-9-]{1,50}$}. A violation is a 400 on field {@code q}.
 */
public record NameQuery(String value) {

    public NameQuery {
        Objects.requireNonNull(value, "value");
        if (!Slug.isValid(value)) {
            throw new InvalidValueException("q", "must be 1-50 letters, digits, spaces or hyphens");
        }
    }

    public static NameQuery parse(String raw) {
        return new NameQuery(Objects.requireNonNull(raw, "raw")
                .strip()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '-'));
    }

    public boolean isPrefixOf(PokemonName name) {
        return name.value().startsWith(value);
    }

    public boolean isContainedIn(PokemonName name) {
        return name.value().contains(value);
    }
}
