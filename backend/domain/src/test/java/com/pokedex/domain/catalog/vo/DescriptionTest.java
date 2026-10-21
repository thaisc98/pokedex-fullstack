package com.pokedex.domain.catalog.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code Description} (BR-03, architecture.md §1.2): {@code \f}, {@code \n} and soft hyphens become spaces,
 * whitespace is collapsed, and the result may be empty. Choosing which flavor text to use is the mapper's job.
 *
 * <p>Invisible characters are built from their code points so the formatter cannot turn them into literals.
 */
class DescriptionTest {

    /** U+00AD SOFT HYPHEN. */
    private static final String SHY = String.valueOf((char) 0x00AD);

    /** U+00A0 NO-BREAK SPACE. */
    private static final String NBSP = String.valueOf((char) 0x00A0);

    @Test
    void should_turn_form_feeds_and_newlines_into_single_spaces() {
        Description description =
                new Description("When several of\nthese POKéMON\fgather, their\felectricity could\nbuild.");

        assertThat(description.value())
                .isEqualTo("When several of these POKéMON gather, their electricity could build.");
    }

    @Test
    void should_turn_a_soft_hyphen_into_a_space() {
        assertThat(new Description("electri" + SHY + "city").value()).isEqualTo("electri city");
    }

    @Test
    void should_collapse_runs_of_whitespace_including_carriage_returns_tabs_and_no_break_spaces() {
        assertThat(new Description("a \r\n\t b" + NBSP + NBSP + "c   d").value())
                .isEqualTo("a b c d");
    }

    @Test
    void should_strip_leading_and_trailing_whitespace() {
        assertThat(new Description("\f  It stores electricity.\n ").value()).isEqualTo("It stores electricity.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\n", "\f\f", "\t\r\n"})
    void should_allow_an_empty_description(String raw) {
        Description description = new Description(raw);

        assertThat(description.value()).isEmpty();
        assertThat(description.isEmpty()).isTrue();
    }

    @Test
    void should_allow_a_description_of_only_soft_hyphens_and_spaces_to_be_empty() {
        assertThat(new Description(SHY).isEmpty()).isTrue();
        assertThat(new Description(" " + SHY + "\n\t " + NBSP).isEmpty()).isTrue();
    }

    @Test
    void should_report_a_non_empty_description() {
        assertThat(new Description("Pika!").isEmpty()).isFalse();
    }

    @Test
    void should_expose_an_empty_constant() {
        assertThat(Description.EMPTY.value()).isEmpty();
        assertThat(new Description("\n")).isEqualTo(Description.EMPTY);
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {"already clean|already clean", "Pokémon 25|Pokémon 25"})
    void should_keep_clean_text_unchanged(String raw, String expected) {
        assertThat(new Description(raw).value()).isEqualTo(expected);
    }

    @Test
    void should_be_idempotent() {
        String once = new Description("a\fb" + SHY + "c\n\nd").value();

        assertThat(once).isEqualTo("a b c d");
        assertThat(new Description(once).value()).isEqualTo(once);
    }

    @Test
    void should_reject_null() {
        assertThatThrownBy(() -> new Description(null)).isInstanceOf(NullPointerException.class);
    }
}
