package com.pokedex.domain.catalog.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.domain.shared.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code PokemonRef}: how {@code /pokemon/{idOrName}} is read (architecture.md §2.8, openapi {@code
 * getPokemonDetail}, testing-strategy §5). All ASCII digits → a Pokédex id; anything else is lowercased and must
 * be a valid {@code PokemonName}. {@code 0} and ids that cannot exist are 404, not 400.
 */
class PokemonRefTest {

    @ParameterizedTest
    @CsvSource({"25, 25", "1, 1", "10264, 10264", "025, 25", "2147483647, 2147483647"})
    void should_read_all_digits_as_a_pokedex_id(String raw, int expectedId) {
        assertThat(PokemonRef.parse(raw)).isEqualTo(new PokemonRef.ById(new PokedexId(expectedId)));
    }

    @ParameterizedTest
    @CsvSource({"pikachu, pikachu", "Pikachu, pikachu", "MR-MIME, mr-mime", "ho-oh, ho-oh", "porygon2, porygon2"})
    void should_lowercase_anything_else_and_read_it_as_a_name(String raw, String expectedName) {
        assertThat(PokemonRef.parse(raw)).isEqualTo(new PokemonRef.ByName(new PokemonName(expectedName)));
    }

    @Test
    void should_read_a_negative_number_as_a_name_so_that_it_ends_as_not_found() {
        // testing-strategy §5: -5 -> 404. "-5" is not all digits but is a well-formed name with no match.
        assertThat(PokemonRef.parse("-5")).isEqualTo(new PokemonRef.ByName(new PokemonName("-5")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "00", "2147483648", "99999999999", "000000000000000000000000000000"})
    void should_treat_zero_and_ids_beyond_int_as_not_found(String raw) {
        assertThatThrownBy(() -> PokemonRef.parse(raw))
                .isInstanceOfSatisfying(
                        NotFoundException.class, e -> assertThat(e.resource()).isEqualTo("pokemon"));
    }

    @Test
    void should_accept_a_very_long_run_of_leading_zeros_before_a_valid_id() {
        assertThat(PokemonRef.parse("0".repeat(40) + "25")).isEqualTo(new PokemonRef.ById(new PokedexId(25)));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"pika;drop", "pika chu", "", " ", "mr.mime", "flabébé", "２５", "25 ", "<script>", "pikachu\n"})
    void should_reject_a_malformed_value_as_invalid(String raw) {
        assertThatThrownBy(() -> PokemonRef.parse(raw)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void should_reject_a_name_longer_than_50_characters() {
        assertThatThrownBy(() -> PokemonRef.parse("a".repeat(51))).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void should_reject_null() {
        assertThatThrownBy(() -> PokemonRef.parse(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_be_sealed_to_id_or_name() {
        assertThat(PokemonRef.class.isSealed()).isTrue();
        assertThat(PokemonRef.class.getPermittedSubclasses())
                .containsExactlyInAnyOrder(PokemonRef.ById.class, PokemonRef.ByName.class);
    }

    @Test
    void should_reject_null_parts() {
        assertThatThrownBy(() -> new PokemonRef.ById(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PokemonRef.ByName(null)).isInstanceOf(NullPointerException.class);
    }
}
