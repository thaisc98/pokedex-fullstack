package com.pokedex.domain.catalog;

import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.BaseStat;
import com.pokedex.domain.catalog.vo.Description;
import com.pokedex.domain.catalog.vo.Height;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.catalog.vo.PokemonType;
import com.pokedex.domain.catalog.vo.StatName;
import com.pokedex.domain.shared.InvalidValueException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The US02 detail (architecture.md §0.1, ui-ux §5.2): exactly one value for each of the six base stats, one or two distinct
 * types, and the abilities (name + hidden flag, as in the summary), all in upstream order.
 */
public record PokemonDetail(
        PokedexId id,
        PokemonName name,
        ImageUrl artwork,
        List<BaseStat> stats,
        Description description,
        EvolutionChain evolution,
        List<PokemonType> types,
        Height height,
        Mass mass,
        List<Ability> abilities) {

    public PokemonDetail {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(artwork, "artwork");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(evolution, "evolution");
        Objects.requireNonNull(height, "height");
        Objects.requireNonNull(mass, "mass");
        stats = List.copyOf(Objects.requireNonNull(stats, "stats"));
        types = List.copyOf(Objects.requireNonNull(types, "types"));
        abilities = List.copyOf(Objects.requireNonNull(abilities, "abilities"));

        Set<StatName> statNames = EnumSet.noneOf(StatName.class);
        stats.forEach(stat -> statNames.add(stat.name()));
        if (stats.size() != StatName.values().length || statNames.size() != stats.size()) {
            throw new InvalidValueException("stats", "must contain each of the six base stats exactly once");
        }
        if (types.isEmpty() || types.size() > 2 || new HashSet<>(types).size() != types.size()) {
            throw new InvalidValueException("types", "must contain one or two distinct types");
        }
    }
}
