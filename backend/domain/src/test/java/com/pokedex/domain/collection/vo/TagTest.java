package com.pokedex.domain.collection.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** {@code Tag}: trimmed, lowercased, {@code ^[a-z0-9-]{1,30}$} (architecture.md §1.2, BR-07). */
class TagTest {

    @ParameterizedTest
    @ValueSource(strings = {"starter", "fire-type", "gen-1", "a", "0"})
    void should_accept_a_normalized_tag(String value) {
        assertThat(new Tag(value).value()).isEqualTo(value);
    }

    @Test
    void should_trim_and_lowercase_the_input() {
        assertThat(new Tag("  Fire-Type \t").value()).isEqualTo("fire-type");
    }

    @Test
    void should_lowercase_with_a_locale_independent_rule() {
        assertThat(new Tag("TITAN").value()).isEqualTo("titan");
    }

    @Test
    void should_consider_tags_equal_after_normalization() {
        assertThat(new Tag(" STARTER ")).isEqualTo(new Tag("starter"));
    }

    @Test
    void should_accept_exactly_30_characters() {
        assertThat(new Tag("a".repeat(30)).value()).hasSize(30);
    }

    @Test
    void should_reject_more_than_30_characters() {
        assertRejected("a".repeat(31));
    }

    @Test
    void should_measure_the_length_after_trimming() {
        assertThat(new Tag("  " + "a".repeat(30) + "  ").value()).hasSize(30);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"", " ", "\t", "fire type", "fire_type", "fire.type", "<script>", "é", "tag;drop", "%", "ı"})
    void should_reject_anything_outside_the_pattern(String value) {
        assertRejected(value);
    }

    @Test
    void should_not_echo_the_rejected_value_in_the_message() {
        assertThatThrownBy(() -> new Tag("<b>evil</b>"))
                .isInstanceOf(InvalidValueException.class)
                .message()
                .doesNotContain("evil");
    }

    @Test
    void should_reject_null() {
        assertThatThrownBy(() -> new Tag(null)).isInstanceOf(NullPointerException.class);
    }

    private static void assertRejected(String value) {
        assertThatThrownBy(() -> new Tag(value))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("tag"));
    }
}
