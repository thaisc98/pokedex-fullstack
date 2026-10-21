package com.pokedex.domain.catalog.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@code Height}: built from decimetres (PokeAPI {@code height}); {@code meters()}; never negative. */
class HeightTest {

    @ParameterizedTest(name = "{0} dm -> {1} m")
    @CsvSource({"4, 0.4", "0, 0.0", "1, 0.1", "10, 1.0", "7, 0.7", "200, 20.0", "1000, 100.0", "2147483647, 214748364.7"
    })
    void should_convert_decimetres_to_meters(int decimetres, double expectedMeters) {
        assertThat(new Height(decimetres).meters()).isEqualTo(expectedMeters);
    }

    @Test
    void should_keep_the_original_decimetres() {
        assertThat(new Height(4).decimetres()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -4, Integer.MIN_VALUE})
    void should_reject_a_negative_height(int decimetres) {
        assertThatThrownBy(() -> new Height(decimetres))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("height"));
    }
}
