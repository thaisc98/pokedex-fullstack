package com.pokedex.domain.catalog.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** {@code PokedexId}: int > 0 (architecture.md §1.2). */
class PokedexIdTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 25, 1025, 10001, Integer.MAX_VALUE})
    void should_accept_a_positive_id(int value) {
        assertThat(new PokedexId(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5, Integer.MIN_VALUE})
    void should_reject_zero_or_a_negative_id(int value) {
        assertThatThrownBy(() -> new PokedexId(value))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("pokedexId"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 25})
    void should_be_equal_when_the_values_are_equal(int value) {
        assertThat(new PokedexId(value)).isEqualTo(new PokedexId(value)).hasSameHashCodeAs(new PokedexId(value));
    }
}
