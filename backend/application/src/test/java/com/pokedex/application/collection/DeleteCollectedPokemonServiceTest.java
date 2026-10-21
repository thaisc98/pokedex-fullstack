package com.pokedex.application.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.application.collection.port.in.DeleteCollectedPokemonUseCase;
import com.pokedex.application.collection.service.DeleteCollectedPokemonService;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.collection.vo.CollectedPokemonId;
import com.pokedex.domain.identity.Role;
import com.pokedex.domain.shared.ForbiddenException;
import com.pokedex.domain.shared.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * UC-05 {@code DeleteCollectedPokemon}: ADMIN only (BR-10). The use case checks the actor's role itself as
 * defence in depth behind the security filter chain (security.md §3).
 */
class DeleteCollectedPokemonServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);
    private static final PokedexId PIKACHU = new PokedexId(25);

    private final InMemoryCollectedPokemonRepository repository = new InMemoryCollectedPokemonRepository();
    private final DeleteCollectedPokemonUseCase useCase = new DeleteCollectedPokemonService(repository);

    @BeforeEach
    void seed() {
        repository.save(CollectedPokemon.collect(
                CollectedPokemonId.random(), CollectionFixtures.snapshot(25, "pikachu"), CLOCK));
    }

    @Test
    void should_delete_the_record_when_the_actor_is_admin() {
        useCase.delete(PIKACHU, Role.ADMIN);

        assertThat(repository.findByPokedexId(PIKACHU)).isEmpty();
    }

    @Test
    void should_forbid_a_user_and_keep_the_record() {
        assertThatThrownBy(() -> useCase.delete(PIKACHU, Role.USER)).isInstanceOf(ForbiddenException.class);

        assertThat(repository.findByPokedexId(PIKACHU)).isPresent();
    }

    @Test
    void should_forbid_a_user_before_revealing_whether_the_record_exists() {
        assertThatThrownBy(() -> useCase.delete(new PokedexId(26), Role.USER)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_throw_not_found_when_an_admin_deletes_a_missing_record() {
        assertThatThrownBy(() -> useCase.delete(new PokedexId(26), Role.ADMIN)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throw_not_found_on_a_second_delete() {
        useCase.delete(PIKACHU, Role.ADMIN);

        assertThatThrownBy(() -> useCase.delete(PIKACHU, Role.ADMIN)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_reject_nulls() {
        assertThatThrownBy(() -> useCase.delete(null, Role.ADMIN)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> useCase.delete(PIKACHU, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DeleteCollectedPokemonService(null)).isInstanceOf(NullPointerException.class);
    }
}
