package com.pokedex.domain.collection;

import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import java.util.List;

/**
 * The single place domain tests build an {@link UpstreamSnapshot}. {@code stats} (List of {@code BaseStat})
 * is added here once US02's {@code BaseStat} reaches main.
 */
final class SnapshotFixtures {

    private SnapshotFixtures() {}

    static UpstreamSnapshot pikachu() {
        return snapshot(25, "pikachu", 60);
    }

    static UpstreamSnapshot snapshot(int id, String name, int hectograms) {
        return new UpstreamSnapshot(
                new PokedexId(id),
                new PokemonName(name),
                ImageUrl.PLACEHOLDER,
                new Category("Mouse Pokémon"),
                new Mass(hectograms),
                List.of(new Ability("static", false), new Ability("lightning-rod", true)));
    }
}
