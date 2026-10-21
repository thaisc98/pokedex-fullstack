package com.pokedex.domain.collection;

import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.BaseStat;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.catalog.vo.StatName;
import java.util.List;

/** The single place domain tests build an {@link UpstreamSnapshot}. */
final class SnapshotFixtures {

    static final List<BaseStat> STATS = List.of(
            new BaseStat(StatName.HP, 35),
            new BaseStat(StatName.ATTACK, 55),
            new BaseStat(StatName.DEFENSE, 40),
            new BaseStat(StatName.SPECIAL_ATTACK, 50),
            new BaseStat(StatName.SPECIAL_DEFENSE, 50),
            new BaseStat(StatName.SPEED, 90));

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
                List.of(new Ability("static", false), new Ability("lightning-rod", true)),
                STATS);
    }
}
