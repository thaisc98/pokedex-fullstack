package com.pokedex.infrastructure.persistence;

import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.BaseStat;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.catalog.vo.StatName;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.collection.CustomAttributes;
import com.pokedex.domain.collection.UpstreamSnapshot;
import com.pokedex.domain.collection.vo.CollectedPokemonId;
import com.pokedex.domain.collection.vo.Coordinates;
import com.pokedex.domain.collection.vo.GeoMetadata;
import com.pokedex.domain.collection.vo.LocalizedName;
import java.time.Instant;
import java.util.List;

final class CollectedPokemonFixtures {

    static final Instant SYNCED = Instant.parse("2026-10-21T10:00:00.123456Z");
    static final Instant UPDATED = Instant.parse("2026-10-21T11:00:00Z");

    static final UpstreamSnapshot PIKACHU = new UpstreamSnapshot(
            new PokedexId(25),
            new PokemonName("pikachu"),
            new ImageUrl("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png"),
            new Category("Mouse Pokémon"),
            new Mass(60),
            List.of(new Ability("static", false), new Ability("lightning-rod", true)),
            List.of(new BaseStat(StatName.HP, 35), new BaseStat(StatName.SPEED, 90)));

    static final CustomAttributes FULL_CUSTOM = CustomAttributes.of(
            List.of(new LocalizedName("ja", "ピカチュウ"), new LocalizedName("pt-BR", "Pikachu")),
            new GeoMetadata("Kanto", "forest", new Coordinates(-90, 180)),
            List.of("starter", "electric"));

    private CollectedPokemonFixtures() {}

    static CollectedPokemon pikachu(CustomAttributes custom, long version) {
        return CollectedPokemon.rehydrate(CollectedPokemonId.random(), PIKACHU, custom, version, SYNCED, UPDATED);
    }
}
