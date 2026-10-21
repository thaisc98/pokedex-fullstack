package com.pokedex.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * Pins the shape of the sealed exception hierarchy that {@code GlobalExceptionHandler} will switch over
 * exhaustively (architecture.md §2.6, backend/AGENTS.md "Errors").
 */
class DomainExceptionTest {

    @Test
    void should_be_a_sealed_unchecked_exception() {
        assertThat(DomainException.class.isSealed()).isTrue();
        assertThat(RuntimeException.class).isAssignableFrom(DomainException.class);
    }

    @Test
    void should_permit_invalid_value_exception_as_a_subtype() {
        assertThat(DomainException.class.getPermittedSubclasses()).contains(InvalidValueException.class);
    }

    @Test
    void should_carry_the_offending_field_and_a_message_when_a_value_is_invalid() {
        DomainException exception = new InvalidValueException("size", "must be between 1 and 100");

        assertThat(exception).isInstanceOf(InvalidValueException.class);
        InvalidValueException invalid = (InvalidValueException) exception;
        assertThat(invalid.field()).isEqualTo("size");
        assertThat(invalid.getMessage()).isEqualTo("must be between 1 and 100");
    }

    @Test
    void should_be_final_so_the_handler_switch_stays_exhaustive() {
        assertThat(Modifier.isFinal(InvalidValueException.class.getModifiers())).isTrue();
    }
}
