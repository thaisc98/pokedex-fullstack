package com.pokedex.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** UC-01 "upstream down → 503": the catalog source could not produce a complete answer. */
class UpstreamUnavailableExceptionTest {

    @Test
    void should_be_a_permitted_domain_exception() {
        assertThat(DomainException.class.getPermittedSubclasses()).contains(UpstreamUnavailableException.class);
    }

    @Test
    void should_keep_the_cause_for_logging() {
        IOException cause = new IOException("connection reset");

        UpstreamUnavailableException exception = new UpstreamUnavailableException("PokeAPI unavailable", cause);

        assertThat(exception).hasMessage("PokeAPI unavailable").hasCause(cause);
    }

    @Test
    void should_be_final_so_the_handler_switch_stays_exhaustive() {
        assertThat(java.lang.reflect.Modifier.isFinal(UpstreamUnavailableException.class.getModifiers()))
                .isTrue();
    }
}
