package com.pokedex.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/** "No such resource" → 404 {@code /problems/not-found}. The message never echoes the requested key. */
class NotFoundExceptionTest {

    @Test
    void should_be_a_final_permitted_domain_exception() {
        assertThat(DomainException.class.getPermittedSubclasses()).contains(NotFoundException.class);
        assertThat(Modifier.isFinal(NotFoundException.class.getModifiers())).isTrue();
    }

    @Test
    void should_name_the_missing_resource_kind() {
        NotFoundException exception = new NotFoundException("pokemon");

        assertThat(exception.resource()).isEqualTo("pokemon");
        assertThat(exception).hasMessage("pokemon not found");
    }

    @Test
    void should_keep_an_optional_cause_for_logging() {
        RuntimeException upstream404 = new RuntimeException("404 from https://pokeapi.co/api/v2/pokemon/99999");

        NotFoundException exception = new NotFoundException("pokemon", upstream404);

        assertThat(exception).hasCause(upstream404).hasMessage("pokemon not found");
    }

    @Test
    void should_reject_a_null_resource() {
        assertThatThrownBy(() -> new NotFoundException(null)).isInstanceOf(NullPointerException.class);
    }
}
