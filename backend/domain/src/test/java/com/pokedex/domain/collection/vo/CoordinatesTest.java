package com.pokedex.domain.collection.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@code Coordinates}: latitude -90..90, longitude -180..180, both inclusive (architecture.md §1.2). */
class CoordinatesTest {

    @ParameterizedTest
    @CsvSource({"-90, -180", "90, 180", "0, 0", "35.6762, 139.6503"})
    void should_accept_coordinates_inside_the_bounds(double latitude, double longitude) {
        Coordinates coordinates = new Coordinates(latitude, longitude);

        assertThat(coordinates.latitude()).isEqualTo(latitude);
        assertThat(coordinates.longitude()).isEqualTo(longitude);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-90.000001, 90.000001, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void should_reject_a_latitude_outside_the_bounds(double latitude) {
        assertThatThrownBy(() -> new Coordinates(latitude, 0))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("coordinates.latitude"));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-180.000001, 180.000001, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void should_reject_a_longitude_outside_the_bounds(double longitude) {
        assertThatThrownBy(() -> new Coordinates(0, longitude))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("coordinates.longitude"));
    }
}
