package com.pokedex.domain.collection.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** {@code CollectedPokemonId}: internal UUID; never exposed by the API (security.md §4.3). */
class CollectedPokemonIdTest {

    @Test
    void should_wrap_a_uuid() {
        UUID uuid = UUID.randomUUID();

        assertThat(new CollectedPokemonId(uuid).value()).isEqualTo(uuid);
    }

    @Test
    void should_generate_distinct_random_ids() {
        assertThat(CollectedPokemonId.random()).isNotEqualTo(CollectedPokemonId.random());
    }

    @Test
    void should_reject_null() {
        assertThatThrownBy(() -> new CollectedPokemonId(null)).isInstanceOf(NullPointerException.class);
    }
}
