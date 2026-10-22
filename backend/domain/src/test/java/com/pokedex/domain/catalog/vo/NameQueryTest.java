package com.pokedex.domain.catalog.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code NameQuery}: the normalized name-search text. Trimmed, lowercased, spaces → hyphens, then {@code
 * ^[a-z0-9-]{1,50}$}; otherwise 400 on field {@code q}.
 */
class NameQueryTest {

    @ParameterizedTest(name = "''{0}'' -> ''{1}''")
    @CsvSource(
            delimiter = '|',
            value = {
                "pika|pika",
                "Pika|pika",
                "  pika  |pika",
                "Mr Mime|mr-mime",
                "mr mime jr|mr-mime-jr",
                "HO-OH|ho-oh",
                "25|25",
                "a|a"
            })
    void should_normalize_the_query(String raw, String expected) {
        assertThat(NameQuery.parse(raw).value()).isEqualTo(expected);
    }

    @Test
    void should_accept_50_characters_after_normalization() {
        assertThat(NameQuery.parse("  " + "a".repeat(50) + "  ").value()).hasSize(50);
    }

    @Test
    void should_reject_51_characters_after_normalization() {
        assertInvalid("a".repeat(51));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "pika;drop", "pika_chu", "pika.chu", "flabébé", "<script>", "pika\tchu", "%"})
    void should_reject_anything_that_is_not_a_slug_after_normalization(String raw) {
        assertInvalid(raw);
    }

    @Test
    void should_reject_null() {
        assertThatThrownBy(() -> NameQuery.parse(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_validate_a_directly_built_query_too() {
        assertThat(new NameQuery("pika").value()).isEqualTo("pika");
        assertThatThrownBy(() -> new NameQuery("Pika"))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("q"));
    }

    @Test
    void should_not_echo_the_rejected_input() {
        assertThatThrownBy(() -> NameQuery.parse("pika;drop")).message().doesNotContain("drop");
    }

    @ParameterizedTest(name = "''{0}'' starts ''{1}'': {2}, contained: {3}")
    @CsvSource({
        "pika, pikachu, true, true",
        "chu, pikachu, false, true",
        "pikachu, pikachu, true, true",
        "raichu, pikachu, false, false",
        "pikachu-, pikachu, false, false"
    })
    void should_tell_prefix_and_containment_matches(String query, String name, boolean prefix, boolean contained) {
        NameQuery q = NameQuery.parse(query);
        PokemonName pokemonName = new PokemonName(name);

        assertThat(q.isPrefixOf(pokemonName)).isEqualTo(prefix);
        assertThat(q.isContainedIn(pokemonName)).isEqualTo(contained);
    }

    private static void assertInvalid(String raw) {
        assertThatThrownBy(() -> NameQuery.parse(raw))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("q"));
    }
}
