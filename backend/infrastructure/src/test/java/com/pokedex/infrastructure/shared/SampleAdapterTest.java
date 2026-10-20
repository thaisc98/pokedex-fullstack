package com.pokedex.infrastructure.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SampleAdapterTest {

    @Test
    void should_return_the_stored_value() {
        assertThat(new SampleAdapter().load()).isEqualTo(21);
    }
}
