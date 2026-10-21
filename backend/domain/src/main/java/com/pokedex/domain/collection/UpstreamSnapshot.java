package com.pokedex.domain.collection;

import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.BaseStat;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import java.util.List;
import java.util.Objects;

/** Read-only copy of the PokeAPI data taken at sync time. Never edited by US04 (BR-06); re-sync replaces it. */
public record UpstreamSnapshot(
        PokedexId id,
        PokemonName name,
        ImageUrl sprite,
        Category category,
        Mass mass,
        List<Ability> abilities,
        List<BaseStat> stats) {

    public UpstreamSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(sprite, "sprite");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(mass, "mass");
        abilities = List.copyOf(Objects.requireNonNull(abilities, "abilities"));
        stats = List.copyOf(Objects.requireNonNull(stats, "stats"));
    }
}
