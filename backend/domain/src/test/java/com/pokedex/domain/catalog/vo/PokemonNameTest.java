package com.pokedex.domain.catalog.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code PokemonName}: lowercase, {@code ^[a-z0-9-]{1,50}$} (architecture.md §1.2).
 *
 * <p>Design choice: the domain <b>rejects</b> anything that does not match; it does not lowercase.
 * Normalizing user input ({@code Pikachu} → {@code pikachu}) is the API layer's job (architecture.md
 * §2.8, openapi {@code /pokemon/{idOrName}}), and PokeAPI names are already lowercase.
 */
class PokemonNameTest {

    @ParameterizedTest
    @ValueSource(strings = {"pikachu", "mr-mime", "ho-oh", "porygon-z", "porygon2", "deoxys-attack", "a", "1"})
    void should_accept_a_lowercase_slug(String value) {
        assertThat(new PokemonName(value).value()).isEqualTo(value);
    }

    @Test
    void should_accept_a_name_of_exactly_fifty_characters() {
        String fifty = "a".repeat(50);

        assertThat(new PokemonName(fifty).value()).hasSize(50);
    }

    @Test
    void should_reject_a_name_of_fifty_one_characters() {
        assertInvalid("a".repeat(51));
    }

    @Test
    void should_reject_an_empty_name() {
        assertInvalid("");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Pikachu", "PIKACHU", "pikaChu"})
    void should_reject_uppercase_instead_of_lowercasing_it(String value) {
        assertInvalid(value);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "pika;drop",
                "pika chu",
                " pikachu",
                "pikachu ",
                "mr_mime",
                "mr.mime",
                "flabébé",
                "nidoran♀",
                "../etc/passwd",
                "pikachu/1",
                "pikachu%20",
                "<script>"
            })
    void should_reject_characters_outside_lowercase_letters_digits_and_hyphen(String value) {
        assertInvalid(value);
    }

    @Test
    void should_reject_a_trailing_newline_so_the_whole_value_must_match() {
        assertInvalid("pikachu\n");
    }

    @Test
    void should_reject_null() {
        assertThatThrownBy(() -> new PokemonName(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_not_echo_the_rejected_input_in_the_message() {
        assertThatThrownBy(() -> new PokemonName("pika;drop"))
                .isInstanceOf(InvalidValueException.class)
                .message()
                .doesNotContain("pika;drop");
    }

    private static void assertInvalid(String value) {
        assertThatThrownBy(() -> new PokemonName(value))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("pokemonName"));
    }
}
