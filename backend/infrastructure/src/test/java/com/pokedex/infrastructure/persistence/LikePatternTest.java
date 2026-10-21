package com.pokedex.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** security.md §4.4: user text inside {@code LIKE} is literal; {@code %}, {@code _} and {@code \} are escaped. */
class LikePatternTest {

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "pika     | %pika%",
                "%        | %\\%%",
                "_        | %\\_%",
                "\\       | %\\\\%",
                "mon-%_\\ | %mon-\\%\\_\\\\%",
                "%%       | %\\%\\%%"
            })
    void should_wrap_in_wildcards_and_escape_like_metacharacters(String input, String expected) {
        assertThat(LikePattern.contains(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"'\\'"})
    void should_use_backslash_as_the_escape_character(String escape) {
        assertThat(LikePattern.ESCAPE).isEqualTo(escape.charAt(0));
    }
}
