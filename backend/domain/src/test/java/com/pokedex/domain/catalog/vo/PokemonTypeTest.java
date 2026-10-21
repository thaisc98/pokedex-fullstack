package com.pokedex.domain.catalog.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@code PokemonType}: the closed set of 18 types, same slugs and order as openapi {@code PokemonType}. */
class PokemonTypeTest {

    @Test
    void should_define_exactly_the_18_types_of_the_contract_in_order() {
        assertThat(PokemonType.values())
                .extracting(PokemonType::slug)
                .containsExactly(
                        "normal",
                        "fire",
                        "water",
                        "electric",
                        "grass",
                        "ice",
                        "fighting",
                        "poison",
                        "ground",
                        "flying",
                        "psychic",
                        "bug",
                        "rock",
                        "ghost",
                        "dragon",
                        "dark",
                        "steel",
                        "fairy");
    }

    @ParameterizedTest
    @EnumSource(PokemonType.class)
    void should_round_trip_every_type_through_its_slug(PokemonType type) {
        assertThat(PokemonType.fromSlug(type.slug())).isSameAs(type);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Fire", "FIRE", "shadow", "unknown", "stellar", "fire "})
    void should_reject_a_slug_outside_the_closed_set(String slug) {
        assertThatThrownBy(() -> PokemonType.fromSlug(slug))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("pokemonType"));
    }

    @Test
    void should_reject_a_null_slug() {
        assertThatThrownBy(() -> PokemonType.fromSlug(null)).isInstanceOf(NullPointerException.class);
    }
}
