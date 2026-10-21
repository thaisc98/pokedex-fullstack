package com.pokedex.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code UserId}: a UUID (architecture.md §2.7 {@code APP_USER.id uuid}). {@code parse} is used for the JWT
 * {@code sub} claim, so bad input is a domain error whose message never echoes the input.
 */
class UserIdTest {

    private static final UUID ID = UUID.fromString("0b6f8f2e-6a43-4b8e-9d0a-6f1d2b3c4d5e");

    @Test
    void should_wrap_a_uuid() {
        assertThat(new UserId(ID).value()).isEqualTo(ID);
    }

    @Test
    void should_generate_distinct_random_ids() {
        assertThat(UserId.random()).isNotEqualTo(UserId.random());
    }

    @Test
    void should_parse_its_string_form() {
        assertThat(UserId.parse(ID.toString())).isEqualTo(new UserId(ID));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-uuid", "1234", "0b6f8f2e-6a43-4b8e-9d0a-6f1d2b3c4d5e-extra"})
    void should_reject_text_that_is_not_a_uuid(String value) {
        assertThatThrownBy(() -> UserId.parse(value))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("userId"))
                .message()
                .doesNotContain("not-a-uuid");
    }

    @Test
    void should_reject_null() {
        assertThatThrownBy(() -> new UserId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> UserId.parse(null)).isInstanceOf(NullPointerException.class);
    }
}
