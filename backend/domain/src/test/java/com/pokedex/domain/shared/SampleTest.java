package com.pokedex.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Surefire unit test. Fully covers {@link Sample} so JaCoCo reports 100% for the domain module and PIT
 * has mutants to kill (the {@code value < 0} guard and the {@code value > 0} comparison).
 */
class SampleTest {

    @Test
    @DisplayName("should reject a negative value")
    void should_reject_a_negative_value() {
        assertThatThrownBy(() -> new Sample(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should report a positive value as positive")
    void should_report_a_positive_value_as_positive() {
        assertThat(new Sample(3).isPositive()).isTrue();
    }

    @Test
    @DisplayName("should report zero as not positive")
    void should_report_zero_as_not_positive() {
        assertThat(new Sample(0).isPositive()).isFalse();
    }
}
