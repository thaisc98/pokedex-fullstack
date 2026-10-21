package com.pokedex.application.collection;

import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.collection.UpstreamSnapshot;
import java.util.List;

/**
 * Builds {@link UpstreamSnapshot}s for application tests. Shipped in the test-jar. {@code stats} is added
 * here once US02's {@code BaseStat} reaches main.
 */
public final class CollectionFixtures {

    private CollectionFixtures() {}

    public static UpstreamSnapshot snapshot(int id, String name) {
        return snapshot(id, name, 60);
    }

    public static UpstreamSnapshot snapshot(int id, String name, int hectograms) {
        return new UpstreamSnapshot(
                new PokedexId(id),
                new PokemonName(name),
                ImageUrl.PLACEHOLDER,
                new Category("Mouse Pokémon"),
                new Mass(hectograms),
                List.of(new Ability("static", false)));
    }
}
