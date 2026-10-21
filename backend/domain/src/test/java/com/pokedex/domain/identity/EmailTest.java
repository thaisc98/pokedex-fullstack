package com.pokedex.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code Email}: trimmed, lowercased, valid format, at most 254 characters (architecture.md §1.2, BR-09,
 * security.md §4.1 "Email normalization").
 *
 * <p>Design choices:
 *
 * <ul>
 *   <li>Unlike {@code PokemonName}, the email is <b>normalized</b> (trim with {@link String#strip()}, then
 *       lowercase with {@link Locale#ROOT}), because the docs say so and the unique index relies on it.
 *   <li>Format: a pragmatic ASCII subset of RFC 5321: a dot-atom local part of 1–64 characters, an
 *       {@code @}, and a domain of at least two DNS labels (letters, digits, inner hyphens). Quoted local
 *       parts, IP-literal domains and internationalized (non-ASCII) addresses are rejected.
 *   <li>The 254 limit applies to the normalized value.
 * </ul>
 */
class EmailTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "ash@example.com",
                "first.last+tag@sub.example.co.uk",
                "o'brien@example.ie",
                "user_name@my-domain.org",
                "a@b.io",
                "1234@123.example"
            })
    void should_accept_a_valid_address(String value) {
        assertThat(new Email(value).value()).isEqualTo(value);
    }

    @Test
    void should_trim_and_lowercase() {
        assertThat(new Email("  Ash.Ketchum@Example.COM \t").value()).isEqualTo("ash.ketchum@example.com");
    }

    @Test
    void should_lowercase_independently_of_the_default_locale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            assertThat(new Email("INFO@EXAMPLE.COM").value()).isEqualTo("info@example.com");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void should_treat_addresses_differing_only_in_case_and_spaces_as_equal() {
        assertThat(new Email(" Brock@Example.com")).isEqualTo(new Email("brock@example.com"));
    }

    @Test
    void should_accept_an_address_of_exactly_254_characters() {
        String email = "a".repeat(64) + "@" + domainOfLength(189);

        assertThat(new Email(email).value()).hasSize(254);
    }

    @Test
    void should_reject_an_address_of_255_characters() {
        assertInvalid("a".repeat(64) + "@" + domainOfLength(190));
    }

    @Test
    void should_apply_the_length_limit_after_trimming() {
        String email = "a".repeat(64) + "@" + domainOfLength(189);

        assertThat(new Email("   " + email + "   ").value()).hasSize(254);
    }

    @Test
    void should_reject_a_local_part_longer_than_64_characters() {
        assertInvalid("a".repeat(65) + "@example.com");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "   ",
                "ash",
                "ash@",
                "@example.com",
                "ash@@example.com",
                "ash@example@example.com",
                "ash@exa mple.com",
                "ash ketchum@example.com",
                "ash\n@example.com",
                "ash@localhost",
                ".ash@example.com",
                "ash.@example.com",
                "a..b@example.com",
                "ash@example..com",
                "ash@.example.com",
                "ash@example.com.",
                "ash@-example.com",
                "ash@example-.com",
                "ash@exam_ple.com",
                "\"ash\"@example.com",
                "ash@[127.0.0.1]",
                "ásh@example.com",
                "ash@exämple.com",
                "<script>@example.com",
                "ash@example.com;drop"
            })
    void should_reject_an_invalid_address(String value) {
        assertInvalid(value);
    }

    @Test
    void should_reject_null() {
        assertThatThrownBy(() -> new Email(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_not_echo_the_rejected_input_in_the_message() {
        assertThatThrownBy(() -> new Email("<script>@example"))
                .isInstanceOf(InvalidValueException.class)
                .message()
                .doesNotContain("<script>");
    }

    private static void assertInvalid(String value) {
        assertThatThrownBy(() -> new Email(value))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("email"));
    }

    /** A valid domain of exactly {@code length} (133–195) characters: three labels of at most 63, then {@code .com}. */
    private static String domainOfLength(int length) {
        return "d".repeat(63) + "." + "d".repeat(63) + "." + "d".repeat(length - 132) + ".com";
    }
}
