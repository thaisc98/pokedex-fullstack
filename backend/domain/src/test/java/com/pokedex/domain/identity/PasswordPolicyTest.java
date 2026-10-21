package com.pokedex.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code PasswordPolicy} (BR-09, security.md §4.1, NIST 800-63B / ASVS): 12–64 characters, any Unicode, no
 * composition rules, rejected if on the common-password list.
 *
 * <p>Design choices:
 *
 * <ul>
 *   <li>The policy is a domain class, but the common-password list is <b>injected</b> as a
 *       {@code Predicate<String>}: the domain stays pure (no file or classpath I/O), and the application layer
 *       adapts its {@code CommonPasswordList} port to it. There is no plaintext-password value object, so the
 *       domain never holds a password beyond this check (BR-09 "the domain only ever holds the hash").
 *   <li>Length counts Unicode code points, not UTF-16 units. The password is <b>not</b> trimmed or
 *       normalized: spaces are legitimate characters.
 *   <li>The password must also fit in 72 UTF-8 bytes, because BCrypt ignores everything after byte 72
 *       (security.md §4.1 caps the length for that reason). Over 72 bytes is reported as "too long", like
 *       over 64 code points.
 *   <li>The list lookup is case-insensitive: the predicate receives the password lowercased with
 *       {@code Locale.ROOT}, so the list is stored lowercase ({@code PASSWORD1234} is as weak as
 *       {@code password1234}).
 *   <li>Length is checked before the list is consulted.
 * </ul>
 */
class PasswordPolicyTest {

    private static final Set<String> COMMON = Set.of("password1234", "qwertyuiop123");

    private final PasswordPolicy policy = new PasswordPolicy(COMMON::contains);

    @Test
    void should_accept_exactly_twelve_characters() {
        assertThatCode(() -> policy.check("Zx9-Qw7-Lm5-")).doesNotThrowAnyException();
    }

    @Test
    void should_accept_exactly_sixty_four_characters() {
        assertThatCode(() -> policy.check("Zx9-Qw7-Lm5-".repeat(5) + "Abcd")).doesNotThrowAnyException();
    }

    @Test
    void should_reject_eleven_characters() {
        assertInvalid("elevenchars");
    }

    @Test
    void should_reject_sixty_five_characters() {
        assertInvalid("a".repeat(65));
    }

    @Test
    void should_reject_an_empty_password() {
        assertInvalid("");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "aaaaaaaaaaaa", // no composition rules
                "correct horse battery staple",
                "alllowercaseletters",
                "123456789012345",
                "contraseña segura",
                "пароль-надёжный",
                "パスワードはとても長いです"
            })
    void should_accept_any_unicode_without_composition_rules(String password) {
        assertThatCode(() -> policy.check(password)).doesNotThrowAnyException();
    }

    @Test
    void should_count_code_points_not_utf16_units() {
        assertInvalid("🔑".repeat(11)); // 22 UTF-16 units, 11 code points
        assertThatCode(() -> policy.check("🔑".repeat(12))).doesNotThrowAnyException(); // 24 units, 48 bytes
    }

    @Test
    void should_accept_exactly_72_utf8_bytes() {
        String seventyTwoBytes = "🔑".repeat(18); // 18 code points x 4 bytes

        assertThatCode(() -> policy.check(seventyTwoBytes)).doesNotThrowAnyException();
    }

    @Test
    void should_reject_more_than_72_utf8_bytes_even_within_64_code_points() {
        // BCrypt only reads the first 72 bytes; a longer password would be silently truncated.
        assertInvalid("🔑".repeat(19)); // 76 bytes, 19 code points
        assertInvalid("é".repeat(37)); // 74 bytes, 37 code points
        assertInvalid("🔑".repeat(64)); // 256 bytes, 64 code points
    }

    @Test
    void should_use_the_too_long_message_for_the_byte_cap() {
        assertThat(messageFor("🔑".repeat(19))).isEqualTo(messageFor("a".repeat(65)));
    }

    @Test
    void should_not_trim_so_spaces_count() {
        assertThatCode(() -> policy.check(" elevenchars")).doesNotThrowAnyException();
    }

    @Test
    void should_reject_a_password_on_the_common_list() {
        assertInvalid("password1234");
    }

    @Test
    void should_reject_a_common_password_regardless_of_case() {
        assertInvalid("PassWord1234");
        assertInvalid("QWERTYUIOP123");
    }

    @Test
    void should_ask_the_list_with_the_lowercased_password() {
        List<String> asked = new ArrayList<>();
        PasswordPolicy recording = new PasswordPolicy(candidate -> {
            asked.add(candidate);
            return false;
        });

        recording.check("Pikachu-THUNDER-42");

        assertThat(asked).containsExactly("pikachu-thunder-42");
    }

    @Test
    void should_not_consult_the_list_when_the_length_is_wrong() {
        List<String> asked = new ArrayList<>();
        PasswordPolicy recording = new PasswordPolicy(candidate -> {
            asked.add(candidate);
            return false;
        });

        assertThatThrownBy(() -> recording.check("short")).isInstanceOf(InvalidValueException.class);

        assertThat(asked).isEmpty();
    }

    @Test
    void should_use_different_messages_for_length_and_common_password() {
        assertThat(messageFor("short")).isNotEqualTo(messageFor("password1234"));
    }

    @Test
    void should_not_echo_the_password_in_the_message() {
        assertThat(messageFor("password1234")).doesNotContainIgnoringCase("password1234");
        assertThat(messageFor("s3cr3t")).doesNotContain("s3cr3t");
    }

    @Test
    void should_reject_a_null_password() {
        assertThatThrownBy(() -> policy.check(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_reject_a_null_common_password_predicate() {
        assertThatThrownBy(() -> new PasswordPolicy(null)).isInstanceOf(NullPointerException.class);
    }

    private String messageFor(String password) {
        try {
            policy.check(password);
        } catch (InvalidValueException e) {
            return e.getMessage();
        }
        throw new AssertionError("expected the password to be rejected");
    }

    private void assertInvalid(String password) {
        assertThatThrownBy(() -> policy.check(password))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("password"));
    }
}
