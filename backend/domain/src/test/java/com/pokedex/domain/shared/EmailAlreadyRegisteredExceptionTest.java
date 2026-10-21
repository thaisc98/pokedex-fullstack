package com.pokedex.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * UC-06 "duplicate → 409" ({@code /problems/email-taken}). Design choice: a specific subtype rather than a
 * generic {@code ConflictException}, because BR-08's stale-version conflict is also a 409 but needs a different
 * problem type, and the handler maps one subtype to one type. It lives in {@code domain.shared} because a
 * sealed class in the unnamed module may only permit subclasses from its own package.
 */
class EmailAlreadyRegisteredExceptionTest {

    @Test
    void should_be_a_permitted_final_domain_exception() {
        assertThat(DomainException.class.getPermittedSubclasses()).contains(EmailAlreadyRegisteredException.class);
        assertThat(Modifier.isFinal(EmailAlreadyRegisteredException.class.getModifiers()))
                .isTrue();
    }

    @Test
    void should_carry_a_fixed_message_without_the_email() {
        assertThat(new EmailAlreadyRegisteredException())
                .hasMessage("email is already registered")
                .hasNoCause();
    }
}
