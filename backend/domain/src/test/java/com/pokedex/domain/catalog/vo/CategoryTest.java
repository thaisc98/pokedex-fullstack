package com.pokedex.domain.catalog.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code Category}: species genus, non-blank, or {@code Category.UNKNOWN} (architecture.md §1.2;
 * testing-strategy §5: genus with no English entry → {@code Category.UNKNOWN}).
 */
class CategoryTest {

    @ParameterizedTest
    @ValueSource(strings = {"Mouse Pokémon", "Seed Pokémon", "Lizard Pokémon", "X"})
    void should_accept_a_non_blank_genus(String value) {
        assertThat(new Category(value).value()).isEqualTo(value);
    }

    @Test
    void should_strip_surrounding_whitespace() {
        assertThat(new Category("  Mouse Pokémon \t").value()).isEqualTo("Mouse Pokémon");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", "\n", " "})
    void should_reject_a_blank_genus(String value) {
        assertThatThrownBy(() -> new Category(value))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("category"));
    }

    @Test
    void should_accept_a_genus_of_exactly_100_characters() {
        assertThat(new Category("a".repeat(100)).value()).hasSize(100);
    }

    @Test
    void should_reject_a_genus_longer_than_100_characters() {
        assertThatThrownBy(() -> new Category("a".repeat(101)))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("category"));
    }

    @Test
    void should_measure_the_length_after_stripping() {
        assertThat(new Category("  " + "a".repeat(100) + "  ").value()).hasSize(100);
    }

    @Test
    void should_reject_null_in_the_constructor() {
        assertThatThrownBy(() -> new Category(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_provide_an_unknown_category_with_a_non_blank_value() {
        assertThat(Category.UNKNOWN.value()).isEqualTo("Unknown");
        assertThat(Category.UNKNOWN.isUnknown()).isTrue();
    }

    @Test
    void should_not_report_a_known_genus_as_unknown() {
        assertThat(new Category("Mouse Pokémon").isUnknown()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void should_fall_back_to_unknown_when_the_genus_is_missing_or_blank(String value) {
        assertThat(Category.orUnknown(value)).isSameAs(Category.UNKNOWN);
    }

    @Test
    void should_keep_a_present_genus_when_falling_back() {
        assertThat(Category.orUnknown("Mouse Pokémon")).isEqualTo(new Category("Mouse Pokémon"));
    }
}
