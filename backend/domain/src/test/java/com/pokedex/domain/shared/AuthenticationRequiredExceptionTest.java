package com.pokedex.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * UC-08 "→ 401" ({@code /problems/unauthorized}): the token was valid but its user no longer exists. Design
 * choice: separate from {@link AuthenticationFailedException}, which is the login failure
 * ({@code /problems/invalid-credentials}).
 */
class AuthenticationRequiredExceptionTest {

    @Test
    void should_be_a_permitted_final_domain_exception() {
        assertThat(DomainException.class.getPermittedSubclasses()).contains(AuthenticationRequiredException.class);
        assertThat(Modifier.isFinal(AuthenticationRequiredException.class.getModifiers()))
                .isTrue();
    }

    @Test
    void should_carry_a_fixed_message() {
        assertThat(new AuthenticationRequiredException())
                .hasMessage("authentication required")
                .hasNoCause();
    }
}
