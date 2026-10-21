package com.pokedex.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.catalog.vo.EvolutionTrigger;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.shared.InvalidValueException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code EvolutionChain} / {@code EvolutionNode} (BR-04): the full tree with branches, in stable (upstream)
 * order. Only the root has no trigger; every later stage has one. Each species appears once.
 */
class EvolutionChainTest {

    private static final EvolutionTrigger LEVEL_16 = EvolutionTrigger.LevelUp.atLevel(16);
    private static final EvolutionTrigger LEVEL_32 = EvolutionTrigger.LevelUp.atLevel(32);

    @Test
    void should_keep_a_linear_three_stage_chain_in_order() {
        EvolutionChain chain =
                new EvolutionChain(root(1, "bulbasaur", node(2, "ivysaur", LEVEL_16, node(3, "venusaur", LEVEL_32))));

        assertThat(chain.root().name().value()).isEqualTo("bulbasaur");
        assertThat(chain.root().trigger()).isNull();
        assertThat(chain.root().evolvesTo()).singleElement().satisfies(ivysaur -> {
            assertThat(ivysaur.trigger()).isEqualTo(LEVEL_16);
            assertThat(ivysaur.evolvesTo()).extracting(n -> n.name().value()).containsExactly("venusaur");
        });
        assertThat(chain.speciesIds()).extracting(PokedexId::value).containsExactly(1, 2, 3);
    }

    @Test
    void should_keep_every_branch_in_upstream_order() {
        // Eevee: 8 branches (BR-04)
        List<EvolutionNode> eeveelutions = List.of(
                node(134, "vaporeon", new EvolutionTrigger.UseItem("water-stone")),
                node(135, "jolteon", new EvolutionTrigger.UseItem("thunder-stone")),
                node(136, "flareon", new EvolutionTrigger.UseItem("fire-stone")),
                node(196, "espeon", EvolutionTrigger.LevelUp.anyLevel()),
                node(197, "umbreon", EvolutionTrigger.LevelUp.anyLevel()),
                node(470, "leafeon", new EvolutionTrigger.Other("use-move")),
                node(471, "glaceon", new EvolutionTrigger.UseItem("ice-stone")),
                node(700, "sylveon", EvolutionTrigger.LevelUp.anyLevel()));

        EvolutionChain chain =
                new EvolutionChain(new EvolutionNode(new PokedexId(133), new PokemonName("eevee"), null, eeveelutions));

        assertThat(chain.root().evolvesTo())
                .extracting(n -> n.name().value())
                .containsExactly(
                        "vaporeon", "jolteon", "flareon", "espeon", "umbreon", "leafeon", "glaceon", "sylveon");
        assertThat(chain.speciesIds()).hasSize(9).first().isEqualTo(new PokedexId(133));
    }

    @Test
    void should_list_species_depth_first_in_stable_order() {
        // root -> [a -> [a1], b]
        EvolutionChain chain = new EvolutionChain(
                root(10, "root", node(11, "a", LEVEL_16, node(12, "a-one", LEVEL_32)), node(13, "b", LEVEL_16)));

        assertThat(chain.speciesIds()).extracting(PokedexId::value).containsExactly(10, 11, 12, 13);
    }

    @Test
    void should_allow_a_pokemon_with_no_evolution() {
        EvolutionChain tauros = new EvolutionChain(root(128, "tauros"));

        assertThat(tauros.root().evolvesTo()).isEmpty();
        assertThat(tauros.speciesIds()).containsExactly(new PokedexId(128));
    }

    @Test
    void should_report_whether_a_species_is_in_the_chain() {
        EvolutionChain chain = new EvolutionChain(root(1, "bulbasaur", node(2, "ivysaur", LEVEL_16)));

        assertThat(chain.contains(new PokedexId(2))).isTrue();
        assertThat(chain.contains(new PokedexId(3))).isFalse();
    }

    @Test
    void should_reject_a_root_with_a_trigger() {
        EvolutionNode rootWithTrigger =
                new EvolutionNode(new PokedexId(1), new PokemonName("bulbasaur"), LEVEL_16, List.of());

        assertThatThrownBy(() -> new EvolutionChain(rootWithTrigger))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("evolution"));
    }

    @Test
    void should_reject_a_later_stage_without_a_trigger() {
        EvolutionNode stageWithoutTrigger =
                new EvolutionNode(new PokedexId(2), new PokemonName("ivysaur"), null, List.of());

        assertThatThrownBy(() -> new EvolutionNode(
                        new PokedexId(1), new PokemonName("bulbasaur"), null, List.of(stageWithoutTrigger)))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("evolution"));
    }

    @Test
    void should_reject_a_species_that_appears_twice() {
        EvolutionNode duplicated = root(1, "a", node(2, "b", LEVEL_16), node(2, "b", LEVEL_32));

        assertThatThrownBy(() -> new EvolutionChain(duplicated))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("evolution"));
    }

    @Test
    void should_reject_the_root_species_appearing_again_deeper() {
        EvolutionNode loop = root(1, "a", node(2, "b", LEVEL_16, node(1, "a", LEVEL_32)));

        assertThatThrownBy(() -> new EvolutionChain(loop)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void should_copy_and_freeze_the_next_stages() {
        List<EvolutionNode> next = new ArrayList<>(List.of(node(2, "ivysaur", LEVEL_16)));
        EvolutionNode bulbasaur = new EvolutionNode(new PokedexId(1), new PokemonName("bulbasaur"), null, next);

        next.clear();

        assertThat(bulbasaur.evolvesTo()).hasSize(1);
        assertThatThrownBy(() -> bulbasaur.evolvesTo().add(node(3, "venusaur", LEVEL_32)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_reject_null_parts() {
        assertThatThrownBy(() -> new EvolutionChain(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EvolutionNode(null, new PokemonName("a"), null, List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EvolutionNode(new PokedexId(1), null, null, List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EvolutionNode(new PokedexId(1), new PokemonName("a"), null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_be_equal_by_value() {
        assertThat(new EvolutionChain(root(1, "a", node(2, "b", LEVEL_16))))
                .isEqualTo(new EvolutionChain(root(1, "a", node(2, "b", LEVEL_16))));
    }

    static EvolutionNode root(int id, String name, EvolutionNode... next) {
        return new EvolutionNode(new PokedexId(id), new PokemonName(name), null, List.of(next));
    }

    static EvolutionNode node(int id, String name, EvolutionTrigger trigger, EvolutionNode... next) {
        return new EvolutionNode(new PokedexId(id), new PokemonName(name), trigger, List.of(next));
    }
}
