package com.pokedex.domain.catalog;

import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.catalog.vo.PokemonType;
import com.pokedex.domain.shared.InvalidValueException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * A US01 catalog list item (architecture.md §0.1: sprite, category, mass, skills), plus one or two distinct types
 * in slot order.
 */
public record PokemonSummary(
        PokedexId id,
        PokemonName name,
        ImageUrl sprite,
        Category category,
        Mass mass,
        List<Ability> abilities,
        List<PokemonType> types) {

    public PokemonSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(sprite, "sprite");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(mass, "mass");
        abilities = List.copyOf(Objects.requireNonNull(abilities, "abilities"));
        types = List.copyOf(Objects.requireNonNull(types, "types"));
        if (types.isEmpty() || types.size() > 2 || new HashSet<>(types).size() != types.size()) {
            throw new InvalidValueException("types", "must contain one or two distinct types");
        }
    }
}
