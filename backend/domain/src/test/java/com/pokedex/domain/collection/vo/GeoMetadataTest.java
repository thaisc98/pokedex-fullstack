package com.pokedex.domain.collection.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** {@code GeoMetadata}: region ≤ 60, habitat ≤ 60, optional {@code Coordinates}; all optional. */
class GeoMetadataTest {

    @Test
    void should_keep_every_field() {
        Coordinates where = new Coordinates(35.0, 139.0);

        GeoMetadata geo = new GeoMetadata("Kanto", "forest", where);

        assertThat(geo.region()).isEqualTo("Kanto");
        assertThat(geo.habitat()).isEqualTo("forest");
        assertThat(geo.coordinates()).isEqualTo(where);
    }

    @Test
    void should_allow_every_field_to_be_absent() {
        GeoMetadata geo = new GeoMetadata(null, null, null);

        assertThat(geo.region()).isNull();
        assertThat(geo.habitat()).isNull();
        assertThat(geo.coordinates()).isNull();
    }

    @Test
    void should_strip_text_fields() {
        GeoMetadata geo = new GeoMetadata("  Kanto ", "\tforest ", null);

        assertThat(geo.region()).isEqualTo("Kanto");
        assertThat(geo.habitat()).isEqualTo("forest");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void should_reject_a_present_but_blank_region(String blank) {
        assertThatThrownBy(() -> new GeoMetadata(blank, null, null))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("geo.region"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void should_reject_a_present_but_blank_habitat(String blank) {
        assertThatThrownBy(() -> new GeoMetadata(null, blank, null))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("geo.habitat"));
    }

    @Test
    void should_accept_1_character() {
        GeoMetadata geo = new GeoMetadata("r", "h", null);

        assertThat(geo.region()).isEqualTo("r");
        assertThat(geo.habitat()).isEqualTo("h");
    }

    @Test
    void should_accept_60_characters() {
        GeoMetadata geo = new GeoMetadata("r".repeat(60), "h".repeat(60), null);

        assertThat(geo.region()).hasSize(60);
        assertThat(geo.habitat()).hasSize(60);
    }

    @Test
    void should_reject_a_region_longer_than_60_characters() {
        assertThatThrownBy(() -> new GeoMetadata("r".repeat(61), null, null))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("geo.region"));
    }

    @Test
    void should_reject_a_habitat_longer_than_60_characters() {
        assertThatThrownBy(() -> new GeoMetadata(null, "h".repeat(61), null))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("geo.habitat"));
    }

    @Test
    void should_report_empty_when_nothing_is_set() {
        assertThat(new GeoMetadata(null, null, null).isEmpty()).isTrue();
        assertThat(new GeoMetadata(null, null, new Coordinates(0, 0)).isEmpty()).isFalse();
    }
}
