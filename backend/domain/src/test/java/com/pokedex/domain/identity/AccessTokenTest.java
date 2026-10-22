package com.pokedex.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * {@code AccessToken}: the output of UC-07 (architecture.md §1.4): an opaque signed token plus its expiry.
 * Never printed, so a logged result cannot leak it (security.md §4.9).
 */
class AccessTokenTest {

    private static final String JWT = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.c2lnbmF0dXJl";
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-21T11:00:00Z");

    @Test
    void should_keep_the_value_and_expiry() {
        AccessToken token = new AccessToken(JWT, EXPIRES_AT);

        assertThat(token.value()).isEqualTo(JWT);
        assertThat(token.expiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void should_print_a_redacted_form_with_the_expiry() {
        assertThat(new AccessToken(JWT, EXPIRES_AT).toString())
                .isEqualTo("AccessToken[value=***, expiresAt=2026-10-21T11:00:00Z]");
    }

    @Test
    void should_never_print_the_token_value() {
        assertThat(new AccessToken(JWT, EXPIRES_AT).toString())
                .doesNotContain(JWT)
                .doesNotContain("eyJ");
    }

    @Test
    void should_reject_a_blank_value() {
        assertThatThrownBy(() -> new AccessToken(" ", EXPIRES_AT))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("accessToken"));
    }

    @Test
    void should_reject_null_parts() {
        assertThatThrownBy(() -> new AccessToken(null, EXPIRES_AT)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AccessToken(JWT, null)).isInstanceOf(NullPointerException.class);
    }
}
