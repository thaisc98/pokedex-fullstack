package com.pokedex.application.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.application.collection.port.in.GetCollectedPokemonUseCase;
import com.pokedex.application.collection.service.GetCollectedPokemonService;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.collection.vo.CollectedPokemonId;
import com.pokedex.domain.shared.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** UC-05 {@code GetCollectedPokemon}: by {@code PokedexId}; missing → 404. */
class GetCollectedPokemonServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);

    private final InMemoryCollectedPokemonRepository repository = new InMemoryCollectedPokemonRepository();
    private final GetCollectedPokemonUseCase useCase = new GetCollectedPokemonService(repository);

    @Test
    void should_return_the_record_for_the_pokedex_id() {
        repository.save(CollectedPokemon.collect(
                CollectedPokemonId.random(), CollectionFixtures.snapshot(25, "pikachu"), CLOCK));

        assertThat(useCase.get(new PokedexId(25)).snapshot().name().value()).isEqualTo("pikachu");
    }

    @Test
    void should_throw_not_found_without_echoing_the_id() {
        assertThatThrownBy(() -> useCase.get(new PokedexId(424242)))
                .isInstanceOf(NotFoundException.class)
                .message()
                .doesNotContain("424242");
    }

    @Test
    void should_reject_nulls() {
        assertThatThrownBy(() -> useCase.get(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetCollectedPokemonService(null)).isInstanceOf(NullPointerException.class);
    }
}
