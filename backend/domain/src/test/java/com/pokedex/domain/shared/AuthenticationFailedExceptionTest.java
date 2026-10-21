package com.pokedex.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * UC-07 "bad credentials → 401 (generic message)" ({@code /problems/invalid-credentials}). One message for every
 * cause (unknown email, wrong password, malformed email), so neither the body nor the log reveals which emails
 * exist (security.md §4.1).
 */
class AuthenticationFailedExceptionTest {

    @Test
    void should_be_a_permitted_final_domain_exception() {
        assertThat(DomainException.class.getPermittedSubclasses()).contains(AuthenticationFailedException.class);
        assertThat(Modifier.isFinal(AuthenticationFailedException.class.getModifiers()))
                .isTrue();
    }

    @Test
    void should_carry_one_generic_message() {
        assertThat(new AuthenticationFailedException())
                .hasMessage("invalid email or password")
                .hasNoCause();
    }
}
