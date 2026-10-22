package com.pokedex.domain.catalog.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** {@code SearchLimit}: 1–20, default 10; otherwise 400 on field {@code limit}. */
class SearchLimitTest {

    @Test
    void should_default_to_10_with_a_maximum_of_20() {
        assertThat(SearchLimit.DEFAULT.value()).isEqualTo(10);
        assertThat(SearchLimit.MAX).isEqualTo(20);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 10, 19, 20})
    void should_accept_1_to_20(int value) {
        assertThat(new SearchLimit(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 21, Integer.MAX_VALUE, Integer.MIN_VALUE})
    void should_reject_values_outside_1_to_20(int value) {
        assertThatThrownBy(() -> new SearchLimit(value)).isInstanceOfSatisfying(InvalidValueException.class, e -> {
            assertThat(e.field()).isEqualTo("limit");
            assertThat(e.getMessage()).isEqualTo("must be between 1 and 20");
        });
    }
}
