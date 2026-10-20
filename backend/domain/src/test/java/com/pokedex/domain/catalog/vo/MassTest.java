package com.pokedex.domain.catalog.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code Mass}: built from hectograms; {@code kilograms()} rounded to 0.1; never negative
 * (architecture.md §1.2, BR-02: 69 hg → 6.9 kg). 0 hg is allowed (MassKg {@code minimum: 0} in
 * openapi.yaml; testing-strategy §5 lists 0 hg and 9999 hg as conversion extremes).
 */
class MassTest {

    @ParameterizedTest(name = "{0} hg -> {1} kg")
    @CsvSource({
        "69,         6.9",
        "0,          0.0",
        "1,          0.1",
        "9,          0.9",
        "10,         1.0",
        "60,         6.0",
        "905,        90.5",
        "9999,       999.9",
        "2147483647, 214748364.7"
    })
    void should_convert_hectograms_to_kilograms_rounded_to_one_decimal(int hectograms, double expectedKilograms) {
        assertThat(new Mass(hectograms).kilograms()).isEqualTo(expectedKilograms);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 7, 69, 123, 4567, 9999, 65535, Integer.MAX_VALUE})
    void should_never_produce_more_than_one_decimal_place(int hectograms) {
        double kilograms = new Mass(hectograms).kilograms();

        assertThat(BigDecimal.valueOf(kilograms).stripTrailingZeros().scale()).isLessThanOrEqualTo(1);
    }

    @Test
    void should_keep_the_original_hectograms() {
        assertThat(new Mass(69).hectograms()).isEqualTo(69);
    }

    @Test
    void should_allow_zero_hectograms() {
        assertThat(new Mass(0).kilograms()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -69, Integer.MIN_VALUE})
    void should_reject_a_negative_mass(int hectograms) {
        assertThatThrownBy(() -> new Mass(hectograms))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("mass"));
    }
}
