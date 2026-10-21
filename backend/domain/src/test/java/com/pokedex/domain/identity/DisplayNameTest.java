package com.pokedex.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code DisplayName}: trimmed, 1–60 characters (architecture.md §1.2).
 *
 * <p>Design choices: "characters" means Unicode code points (an emoji counts once), the same rule as the
 * password policy; trimming uses {@link String#strip()}; ISO control characters (newline, tab, NUL…) are
 * rejected so a display name can never forge a log line or break a layout. Inner spaces are kept as typed.
 */
class DisplayNameTest {

    @ParameterizedTest
    @ValueSource(strings = {"Ash Ketchum", "A", "Professor Oak", "サトシ", "Ash 🔥", "O'Brien-Smith"})
    void should_accept_a_valid_name(String value) {
        assertThat(new DisplayName(value).value()).isEqualTo(value);
    }

    @Test
    void should_trim_surrounding_whitespace() {
        assertThat(new DisplayName("  Ash Ketchum  ").value()).isEqualTo("Ash Ketchum");
    }

    @Test
    void should_keep_inner_spaces_as_typed() {
        assertThat(new DisplayName("Ash  Ketchum").value()).isEqualTo("Ash  Ketchum");
    }

    @Test
    void should_accept_exactly_sixty_characters() {
        assertThat(new DisplayName("a".repeat(60)).value()).hasSize(60);
    }

    @Test
    void should_reject_sixty_one_characters() {
        assertInvalid("a".repeat(61));
    }

    @Test
    void should_apply_the_length_limit_after_trimming() {
        assertThat(new DisplayName("  " + "a".repeat(60) + "  ").value()).hasSize(60);
    }

    @Test
    void should_count_code_points_not_utf16_units() {
        String sixtyEmoji = "🔥".repeat(60); // 120 UTF-16 units

        assertThat(new DisplayName(sixtyEmoji).value()).isEqualTo(sixtyEmoji);
        assertInvalid("🔥".repeat(61));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", " "})
    void should_reject_an_empty_or_blank_name(String value) {
        assertInvalid(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ash\nKetchum", "Ash\rKetchum", "Ash\tKetchum", "Ash\u0000", "Ash\u001b[31m", "Ash\u007f"})
    void should_reject_control_characters(String value) {
        assertInvalid(value);
    }

    @Test
    void should_reject_null() {
        assertThatThrownBy(() -> new DisplayName(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_not_echo_the_rejected_input_in_the_message() {
        assertThatThrownBy(() -> new DisplayName("Evil\nINFO forged"))
                .isInstanceOf(InvalidValueException.class)
                .message()
                .doesNotContain("Evil")
                .doesNotContain("forged");
    }

    private static void assertInvalid(String value) {
        assertThatThrownBy(() -> new DisplayName(value))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("displayName"));
    }
}
