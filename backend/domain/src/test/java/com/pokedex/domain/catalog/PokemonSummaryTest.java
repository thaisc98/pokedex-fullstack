package com.pokedex.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@code PokemonSummary}: the US01 list item composing the catalog value objects (architecture.md §1.2). */
class PokemonSummaryTest {

    private static final PokedexId ID = new PokedexId(25);
    private static final PokemonName NAME = new PokemonName("pikachu");
    private static final ImageUrl SPRITE =
            new ImageUrl("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png");
    private static final Category CATEGORY = new Category("Mouse Pokémon");
    private static final Mass MASS = new Mass(60);
    private static final List<Ability> ABILITIES =
            List.of(new Ability("static", false), new Ability("lightning-rod", true));

    @Test
    void should_compose_id_name_sprite_category_mass_and_abilities() {
        PokemonSummary summary = new PokemonSummary(ID, NAME, SPRITE, CATEGORY, MASS, ABILITIES);

        assertThat(summary.id()).isEqualTo(ID);
        assertThat(summary.name()).isEqualTo(NAME);
        assertThat(summary.sprite()).isEqualTo(SPRITE);
        assertThat(summary.category()).isEqualTo(CATEGORY);
        assertThat(summary.mass().kilograms()).isEqualTo(6.0);
        assertThat(summary.abilities()).containsExactlyElementsOf(ABILITIES);
    }

    @Test
    void should_accept_an_unknown_category() {
        PokemonSummary summary = new PokemonSummary(ID, NAME, SPRITE, Category.UNKNOWN, MASS, ABILITIES);

        assertThat(summary.category().isUnknown()).isTrue();
    }

    @Test
    void should_allow_an_empty_ability_list() {
        PokemonSummary summary = new PokemonSummary(ID, NAME, SPRITE, CATEGORY, MASS, List.of());

        assertThat(summary.abilities()).isEmpty();
    }

    @Test
    void should_keep_ability_order_and_copy_the_list_defensively() {
        List<Ability> source = new ArrayList<>(ABILITIES);
        PokemonSummary summary = new PokemonSummary(ID, NAME, SPRITE, CATEGORY, MASS, source);

        source.clear();

        assertThat(summary.abilities())
                .containsExactly(new Ability("static", false), new Ability("lightning-rod", true));
    }

    @Test
    void should_expose_an_unmodifiable_ability_list() {
        PokemonSummary summary = new PokemonSummary(ID, NAME, SPRITE, CATEGORY, MASS, ABILITIES);

        assertThatThrownBy(() -> summary.abilities().add(new Ability("pressure", false)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_reject_null_components() {
        assertThatThrownBy(() -> new PokemonSummary(null, NAME, SPRITE, CATEGORY, MASS, ABILITIES))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PokemonSummary(ID, null, SPRITE, CATEGORY, MASS, ABILITIES))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PokemonSummary(ID, NAME, null, CATEGORY, MASS, ABILITIES))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PokemonSummary(ID, NAME, SPRITE, null, MASS, ABILITIES))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PokemonSummary(ID, NAME, SPRITE, CATEGORY, null, ABILITIES))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PokemonSummary(ID, NAME, SPRITE, CATEGORY, MASS, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_reject_a_null_ability_in_the_list() {
        List<Ability> withNull = new ArrayList<>();
        withNull.add(new Ability("static", false));
        withNull.add(null);

        assertThatThrownBy(() -> new PokemonSummary(ID, NAME, SPRITE, CATEGORY, MASS, withNull))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_be_equal_when_all_components_are_equal() {
        assertThat(new PokemonSummary(ID, NAME, SPRITE, CATEGORY, MASS, ABILITIES))
                .isEqualTo(new PokemonSummary(
                        new PokedexId(25),
                        new PokemonName("pikachu"),
                        SPRITE,
                        new Category("Mouse Pokémon"),
                        new Mass(60),
                        new ArrayList<>(ABILITIES)));
    }
}
