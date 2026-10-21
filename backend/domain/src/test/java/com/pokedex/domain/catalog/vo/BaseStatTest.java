package com.pokedex.domain.catalog.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@code BaseStat}: name ∈ the six core stats, value 1–255 (architecture.md §1.2, openapi {@code BaseStat}). */
class BaseStatTest {

    @Test
    void should_define_exactly_the_six_core_stats_in_canonical_order() {
        assertThat(StatName.values())
                .extracting(StatName::slug)
                .containsExactly("hp", "attack", "defense", "special-attack", "special-defense", "speed");
    }

    @ParameterizedTest
    @CsvSource({
        "hp, HP",
        "attack, ATTACK",
        "defense, DEFENSE",
        "special-attack, SPECIAL_ATTACK",
        "special-defense, SPECIAL_DEFENSE",
        "speed, SPEED"
    })
    void should_resolve_a_stat_from_its_pokeapi_slug(String slug, StatName expected) {
        assertThat(StatName.fromSlug(slug)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "HP", "accuracy", "evasion", "special_attack", "sp-atk", " hp"})
    void should_reject_an_unknown_stat_slug(String slug) {
        assertThatThrownBy(() -> StatName.fromSlug(slug))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("stat"));
    }

    @Test
    void should_reject_a_null_stat_slug() {
        assertThatThrownBy(() -> StatName.fromSlug(null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 35, 254, 255})
    void should_accept_a_value_between_1_and_255(int value) {
        BaseStat stat = new BaseStat(StatName.HP, value);

        assertThat(stat.name()).isEqualTo(StatName.HP);
        assertThat(stat.value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 256, Integer.MAX_VALUE, Integer.MIN_VALUE})
    void should_reject_a_value_outside_1_to_255(int value) {
        assertThatThrownBy(() -> new BaseStat(StatName.SPEED, value))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("stat"));
    }

    @Test
    void should_reject_a_null_name() {
        assertThatThrownBy(() -> new BaseStat(null, 10)).isInstanceOf(NullPointerException.class);
    }
}
