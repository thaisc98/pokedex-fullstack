package com.pokedex.domain.collection.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code LocalizedName}: BCP-47 language tag checked with {@code Locale.forLanguageTag} plus a 1-60
 * character value (architecture.md §1.2).
 */
class LocalizedNameTest {

    @ParameterizedTest
    @ValueSource(strings = {"en", "ja", "fr", "pt-BR", "zh-Hant", "es-419"})
    void should_accept_a_well_formed_language_tag(String lang) {
        assertThat(new LocalizedName(lang, "Pikachu").lang()).isEqualTo(lang);
    }

    @ParameterizedTest
    @CsvSource({"pt-br, pt-BR", "EN, en", "zh-hant, zh-Hant", "' ja ', ja"})
    void should_store_the_canonical_form_of_the_language_tag(String input, String canonical) {
        assertThat(new LocalizedName(input, "Pikachu").lang()).isEqualTo(canonical);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "und", "e", "english!", "en_US", "12", "en--us", "<script>"})
    void should_reject_a_malformed_language_tag(String lang) {
        assertThatThrownBy(() -> new LocalizedName(lang, "Pikachu"))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("localizedName.lang"));
    }

    @Test
    void should_reject_a_language_tag_longer_than_35_characters() {
        assertThatThrownBy(() -> new LocalizedName("en-" + "abcdefgh-".repeat(3) + "abcdefgh", "Pikachu"))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("localizedName.lang"));
    }

    @Test
    void should_accept_a_non_latin_value() {
        assertThat(new LocalizedName("ja", "ピカチュウ").value()).isEqualTo("ピカチュウ");
    }

    @Test
    void should_strip_the_value() {
        assertThat(new LocalizedName("en", "  Pikachu ").value()).isEqualTo("Pikachu");
    }

    @Test
    void should_accept_a_value_of_exactly_60_characters() {
        assertThat(new LocalizedName("en", "a".repeat(60)).value()).hasSize(60);
    }

    @Test
    void should_count_characters_not_utf16_units() {
        String sixtyEmoji = "😀".repeat(60);

        assertThat(new LocalizedName("en", sixtyEmoji).value()).isEqualTo(sixtyEmoji);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void should_reject_a_blank_value(String value) {
        assertRejectedValue(value);
    }

    @Test
    void should_reject_a_value_longer_than_60_characters() {
        assertRejectedValue("a".repeat(61));
    }

    @Test
    void should_keep_markup_as_plain_text() {
        assertThat(new LocalizedName("en", "<b>Pika</b>").value()).isEqualTo("<b>Pika</b>");
    }

    @Test
    void should_not_echo_the_rejected_value_in_the_message() {
        assertThatThrownBy(() -> new LocalizedName("evil-input-here!", "x"))
                .isInstanceOf(InvalidValueException.class)
                .message()
                .doesNotContain("evil");
    }

    @Test
    void should_reject_nulls() {
        assertThatThrownBy(() -> new LocalizedName(null, "x")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LocalizedName("en", null)).isInstanceOf(NullPointerException.class);
    }

    private static void assertRejectedValue(String value) {
        assertThatThrownBy(() -> new LocalizedName("en", value))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("localizedName.value"));
    }
}
