package com.pokedex.domain.catalog.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code Ability}: name plus a {@code hidden} flag (architecture.md §1.2). The name is a PokeAPI slug
 * (openapi {@code Ability.name}), so it uses the same {@code ^[a-z0-9-]{1,50}$} rule as {@code
 * PokemonName}; upstream data must pass domain checks (security.md T8 / §4.5).
 */
class AbilityTest {

    @Test
    void should_keep_name_and_hidden_flag() {
        Ability ability = new Ability("lightning-rod", true);

        assertThat(ability.name()).isEqualTo("lightning-rod");
        assertThat(ability.hidden()).isTrue();
    }

    @Test
    void should_represent_a_regular_ability_as_not_hidden() {
        assertThat(new Ability("static", false).hidden()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"static", "lightning-rod", "as-one-glastrier", "a"})
    void should_accept_a_slug_name(String name) {
        assertThat(new Ability(name, false).name()).isEqualTo(name);
    }

    @Test
    void should_accept_a_name_of_exactly_fifty_characters() {
        assertThat(new Ability("a".repeat(50), false).name()).hasSize(50);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "Static", "lightning rod", "lightning_rod", "<b>static</b>", "static\n"})
    void should_reject_a_name_that_is_not_a_lowercase_slug(String name) {
        assertThatThrownBy(() -> new Ability(name, false))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("ability"));
    }

    @Test
    void should_reject_a_name_of_fifty_one_characters() {
        assertThatThrownBy(() -> new Ability("a".repeat(51), false)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void should_reject_a_null_name() {
        assertThatThrownBy(() -> new Ability(null, false)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_distinguish_the_same_ability_by_hidden_flag() {
        assertThat(new Ability("static", true)).isNotEqualTo(new Ability("static", false));
    }
}
