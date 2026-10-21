package com.pokedex.application.catalog;

import com.pokedex.domain.catalog.EvolutionChain;
import com.pokedex.domain.catalog.EvolutionNode;
import com.pokedex.domain.catalog.PokemonDetail;
import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.BaseStat;
import com.pokedex.domain.catalog.vo.Description;
import com.pokedex.domain.catalog.vo.EvolutionTrigger;
import com.pokedex.domain.catalog.vo.Height;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.catalog.vo.PokemonType;
import com.pokedex.domain.catalog.vo.StatName;
import java.util.List;

/** Ready-made {@link PokemonDetail}s for tests in any module (shipped in application's test-jar). */
public final class PokemonDetailFixtures {

    private PokemonDetailFixtures() {}

    public static PokemonDetail pikachu() {
        EvolutionChain chain = new EvolutionChain(new EvolutionNode(
                new PokedexId(172),
                new PokemonName("pichu"),
                null,
                List.of(new EvolutionNode(
                        new PokedexId(25),
                        new PokemonName("pikachu"),
                        EvolutionTrigger.LevelUp.anyLevel(),
                        List.of(new EvolutionNode(
                                new PokedexId(26),
                                new PokemonName("raichu"),
                                new EvolutionTrigger.UseItem("thunder-stone"),
                                List.of()))))));
        return new PokemonDetail(
                new PokedexId(25),
                new PokemonName("pikachu"),
                new ImageUrl("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/"
                        + "official-artwork/25.png"),
                List.of(
                        new BaseStat(StatName.HP, 35),
                        new BaseStat(StatName.ATTACK, 55),
                        new BaseStat(StatName.DEFENSE, 40),
                        new BaseStat(StatName.SPECIAL_ATTACK, 50),
                        new BaseStat(StatName.SPECIAL_DEFENSE, 50),
                        new BaseStat(StatName.SPEED, 90)),
                new Description("When several of these POKéMON gather, their electricity could build and cause"
                        + " lightning storms."),
                chain,
                List.of(PokemonType.ELECTRIC),
                new Height(4),
                new Mass(60),
                List.of(new Ability("static", false), new Ability("lightning-rod", true)));
    }
}
