package com.pokedex.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code PasswordHash}: an opaque, encoder-prefixed hash ({@code {bcrypt}$2a$12$…}). The domain never parses
 * it; it only requires a non-blank value and never prints it (security.md §4.9: no secrets in logs).
 */
class PasswordHashTest {

    private static final String BCRYPT = "{bcrypt}$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW";

    @Test
    void should_keep_the_value_as_given() {
        assertThat(new PasswordHash(BCRYPT).value()).isEqualTo(BCRYPT);
    }

    @Test
    void should_be_equal_by_value() {
        assertThat(new PasswordHash(BCRYPT)).isEqualTo(new PasswordHash(BCRYPT));
    }

    @Test
    void should_never_print_the_hash() {
        String printed = new PasswordHash(BCRYPT).toString();

        assertThat(printed).doesNotContain(BCRYPT).doesNotContain("$2a$").doesNotContain("R9h/cIPz0gi");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void should_reject_a_blank_value(String value) {
        assertThatThrownBy(() -> new PasswordHash(value))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("passwordHash"));
    }

    @Test
    void should_reject_null() {
        assertThatThrownBy(() -> new PasswordHash(null)).isInstanceOf(NullPointerException.class);
    }
}
