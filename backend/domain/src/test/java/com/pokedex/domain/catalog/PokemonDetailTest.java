package com.pokedex.domain.catalog;

import static com.pokedex.domain.catalog.EvolutionChainTest.node;
import static com.pokedex.domain.catalog.EvolutionChainTest.root;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.pokedex.domain.shared.InvalidValueException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code PokemonDetail}: the US02 detail (architecture.md §0.1, §1.2): id, name, artwork, the six base stats,
 * description, evolution chain, types, height and mass.
 */
class PokemonDetailTest {

    private static final ImageUrl ARTWORK = new ImageUrl(
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/25.png");
    private static final List<Ability> ABILITIES =
            List.of(new Ability("static", false), new Ability("lightning-rod", true));
    private static final EvolutionChain CHAIN = new EvolutionChain(root(
            172,
            "pichu",
            node(
                    25,
                    "pikachu",
                    EvolutionTrigger.LevelUp.anyLevel(),
                    node(26, "raichu", new EvolutionTrigger.UseItem("thunder-stone")))));

    static List<BaseStat> sixStats() {
        return List.of(
                new BaseStat(StatName.HP, 35),
                new BaseStat(StatName.ATTACK, 55),
                new BaseStat(StatName.DEFENSE, 40),
                new BaseStat(StatName.SPECIAL_ATTACK, 50),
                new BaseStat(StatName.SPECIAL_DEFENSE, 50),
                new BaseStat(StatName.SPEED, 90));
    }

    static PokemonDetail pikachu() {
        return new PokemonDetail(
                new PokedexId(25),
                new PokemonName("pikachu"),
                ARTWORK,
                sixStats(),
                new Description("When several of\nthese POKéMON\fgather."),
                CHAIN,
                List.of(PokemonType.ELECTRIC),
                new Height(4),
                new Mass(60),
                ABILITIES);
    }

    @Test
    void should_compose_every_part_of_the_detail() {
        PokemonDetail detail = pikachu();

        assertThat(detail.id().value()).isEqualTo(25);
        assertThat(detail.name().value()).isEqualTo("pikachu");
        assertThat(detail.artwork()).isEqualTo(ARTWORK);
        assertThat(detail.stats()).extracting(BaseStat::name).containsExactly(StatName.values());
        assertThat(detail.description().value()).isEqualTo("When several of these POKéMON gather.");
        assertThat(detail.evolution()).isEqualTo(CHAIN);
        assertThat(detail.types()).containsExactly(PokemonType.ELECTRIC);
        assertThat(detail.height().meters()).isEqualTo(0.4);
        assertThat(detail.mass().kilograms()).isEqualTo(6.0);
    }

    @Test
    void should_keep_the_upstream_order_of_stats_and_types() {
        List<BaseStat> reversed = new ArrayList<>(sixStats()).reversed();
        PokemonDetail detail = new PokemonDetail(
                new PokedexId(6),
                new PokemonName("charizard"),
                ImageUrl.PLACEHOLDER,
                reversed,
                Description.EMPTY,
                new EvolutionChain(root(6, "charizard")),
                List.of(PokemonType.FIRE, PokemonType.FLYING),
                new Height(17),
                new Mass(905),
                List.of(new Ability("blaze", false), new Ability("solar-power", true)));

        assertThat(detail.stats()).containsExactlyElementsOf(reversed);
        assertThat(detail.types()).containsExactly(PokemonType.FIRE, PokemonType.FLYING);
    }

    @Test
    void should_allow_an_empty_description_and_the_placeholder_artwork() {
        PokemonDetail detail = withArtworkAndDescription(ImageUrl.PLACEHOLDER, Description.EMPTY);

        assertThat(detail.description().isEmpty()).isTrue();
        assertThat(detail.artwork()).isSameAs(ImageUrl.PLACEHOLDER);
    }

    @Test
    void should_require_exactly_one_value_for_each_of_the_six_stats() {
        List<BaseStat> five = sixStats().subList(0, 5);
        List<BaseStat> duplicated = new ArrayList<>(sixStats());
        duplicated.set(5, new BaseStat(StatName.HP, 10));
        List<BaseStat> seven = new ArrayList<>(sixStats());
        seven.add(new BaseStat(StatName.SPEED, 1));

        for (List<BaseStat> stats : List.of(five, duplicated, seven, List.<BaseStat>of())) {
            assertThatThrownBy(() -> withStats(stats))
                    .isInstanceOfSatisfying(
                            InvalidValueException.class,
                            e -> assertThat(e.field()).isEqualTo("stats"));
        }
    }

    @Test
    void should_require_one_or_two_distinct_types() {
        for (List<PokemonType> types : List.of(
                List.<PokemonType>of(),
                List.of(PokemonType.FIRE, PokemonType.FIRE),
                List.of(PokemonType.FIRE, PokemonType.FLYING, PokemonType.DRAGON))) {
            assertThatThrownBy(() -> withTypes(types))
                    .isInstanceOfSatisfying(
                            InvalidValueException.class,
                            e -> assertThat(e.field()).isEqualTo("types"));
        }
    }

    @Test
    void should_copy_and_freeze_stats_and_types() {
        List<BaseStat> stats = new ArrayList<>(sixStats());
        List<PokemonType> types = new ArrayList<>(List.of(PokemonType.ELECTRIC));
        PokemonDetail detail = new PokemonDetail(
                new PokedexId(25),
                new PokemonName("pikachu"),
                ARTWORK,
                stats,
                Description.EMPTY,
                CHAIN,
                types,
                new Height(4),
                new Mass(60),
                ABILITIES);

        stats.clear();
        types.add(PokemonType.FAIRY);

        assertThat(detail.stats()).hasSize(6);
        assertThat(detail.types()).containsExactly(PokemonType.ELECTRIC);
        assertThatThrownBy(() -> detail.types().add(PokemonType.FIRE))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_carry_the_abilities_in_upstream_order() {
        assertThat(pikachu().abilities())
                .containsExactly(new Ability("static", false), new Ability("lightning-rod", true));
    }

    @Test
    void should_allow_an_empty_ability_list() {
        PokemonDetail detail = new PokemonDetail(
                new PokedexId(25),
                new PokemonName("pikachu"),
                ARTWORK,
                sixStats(),
                Description.EMPTY,
                CHAIN,
                List.of(PokemonType.ELECTRIC),
                new Height(4),
                new Mass(60),
                List.of());

        assertThat(detail.abilities()).isEmpty();
    }

    @Test
    void should_copy_and_freeze_the_abilities() {
        List<Ability> abilities = new ArrayList<>(ABILITIES);
        PokemonDetail detail = new PokemonDetail(
                new PokedexId(25),
                new PokemonName("pikachu"),
                ARTWORK,
                sixStats(),
                Description.EMPTY,
                CHAIN,
                List.of(PokemonType.ELECTRIC),
                new Height(4),
                new Mass(60),
                abilities);

        abilities.clear();

        assertThat(detail.abilities()).hasSize(2);
        assertThatThrownBy(() -> detail.abilities().add(new Ability("pressure", false)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_reject_null_abilities() {
        assertThatThrownBy(() -> new PokemonDetail(
                        new PokedexId(25),
                        new PokemonName("pikachu"),
                        ARTWORK,
                        sixStats(),
                        Description.EMPTY,
                        CHAIN,
                        List.of(PokemonType.ELECTRIC),
                        new Height(4),
                        new Mass(60),
                        null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_reject_null_components() {
        assertThatThrownBy(() -> new PokemonDetail(
                        null,
                        new PokemonName("pikachu"),
                        ARTWORK,
                        sixStats(),
                        Description.EMPTY,
                        CHAIN,
                        List.of(PokemonType.ELECTRIC),
                        new Height(4),
                        new Mass(60),
                        ABILITIES))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PokemonDetail(
                        new PokedexId(25),
                        new PokemonName("pikachu"),
                        null,
                        sixStats(),
                        Description.EMPTY,
                        CHAIN,
                        List.of(PokemonType.ELECTRIC),
                        new Height(4),
                        new Mass(60),
                        ABILITIES))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PokemonDetail(
                        new PokedexId(25),
                        new PokemonName("pikachu"),
                        ARTWORK,
                        null,
                        Description.EMPTY,
                        CHAIN,
                        List.of(PokemonType.ELECTRIC),
                        new Height(4),
                        new Mass(60),
                        ABILITIES))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PokemonDetail(
                        new PokedexId(25),
                        new PokemonName("pikachu"),
                        ARTWORK,
                        sixStats(),
                        Description.EMPTY,
                        null,
                        List.of(PokemonType.ELECTRIC),
                        new Height(4),
                        new Mass(60),
                        ABILITIES))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PokemonDetail(
                        new PokedexId(25),
                        new PokemonName("pikachu"),
                        ARTWORK,
                        sixStats(),
                        Description.EMPTY,
                        CHAIN,
                        null,
                        new Height(4),
                        new Mass(60),
                        ABILITIES))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PokemonDetail(
                        new PokedexId(25),
                        new PokemonName("pikachu"),
                        ARTWORK,
                        sixStats(),
                        Description.EMPTY,
                        CHAIN,
                        List.of(PokemonType.ELECTRIC),
                        new Height(4),
                        null))
                .isInstanceOf(NullPointerException.class);
    }

    private static PokemonDetail withStats(List<BaseStat> stats) {
        return new PokemonDetail(
                new PokedexId(25),
                new PokemonName("pikachu"),
                ARTWORK,
                stats,
                Description.EMPTY,
                CHAIN,
                List.of(PokemonType.ELECTRIC),
                new Height(4),
                new Mass(60),
                ABILITIES);
    }

    private static PokemonDetail withTypes(List<PokemonType> types) {
        return new PokemonDetail(
                new PokedexId(25),
                new PokemonName("pikachu"),
                ARTWORK,
                sixStats(),
                Description.EMPTY,
                CHAIN,
                types,
                new Height(4),
                new Mass(60),
                ABILITIES);
    }

    private static PokemonDetail withArtworkAndDescription(ImageUrl artwork, Description description) {
        return new PokemonDetail(
                new PokedexId(25),
                new PokemonName("pikachu"),
                artwork,
                sixStats(),
                description,
                CHAIN,
                List.of(PokemonType.ELECTRIC),
                new Height(4),
                new Mass(60),
                ABILITIES);
    }
}
