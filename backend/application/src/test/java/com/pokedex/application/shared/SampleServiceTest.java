package com.pokedex.application.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Surefire unit test proving Mockito (ByteBuddy) runs on Java 25 bytecode. Also fully covers
 * {@link SampleService}, so JaCoCo and PIT have something to work with.
 */
@ExtendWith(MockitoExtension.class)
class SampleServiceTest {

    @Mock
    private SampleGateway gateway;

    @Test
    @DisplayName("should double the value loaded from the gateway")
    void should_double_the_value_loaded_from_the_gateway() {
        when(gateway.load()).thenReturn(21);

        int result = new SampleService(gateway).doubled();

        assertThat(result).isEqualTo(42);
        verify(gateway).load();
    }

    @Test
    @DisplayName("should work with the in-memory fake from the test-jar")
    void should_work_with_the_in_memory_fake() {
        assertThat(new SampleService(new InMemorySampleGateway(5)).doubled()).isEqualTo(10);
    }
}
