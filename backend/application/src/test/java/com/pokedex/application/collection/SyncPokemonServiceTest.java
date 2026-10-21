package com.pokedex.application.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.application.collection.command.CollectionQuery;
import com.pokedex.application.collection.port.in.SyncPokemonUseCase;
import com.pokedex.application.collection.port.in.SyncResult;
import com.pokedex.application.collection.service.SyncPokemonService;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.collection.CustomAttributes;
import com.pokedex.domain.shared.NotFoundException;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** UC-03 {@code SyncPokemon}: idempotent upsert keyed by {@code PokedexId} (BR-05). */
class SyncPokemonServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-21T10:00:00Z");
    private static final PokedexId PIKACHU = new PokedexId(25);

    private final MutableClock clock = new MutableClock(T0);
    private final FakeUpstreamSnapshotPort upstream =
            new FakeUpstreamSnapshotPort().with(CollectionFixtures.snapshot(25, "pikachu", 60));
    private final InMemoryCollectedPokemonRepository repository = new InMemoryCollectedPokemonRepository();
    private final SyncPokemonUseCase useCase = new SyncPokemonService(upstream, repository, clock);

    @Test
    void should_create_a_new_record_with_empty_custom_attributes_on_first_sync() {
        SyncResult result = useCase.sync(PIKACHU);

        assertThat(result.created()).isTrue();
        CollectedPokemon pokemon = result.pokemon();
        assertThat(pokemon.pokedexId()).isEqualTo(PIKACHU);
        assertThat(pokemon.custom()).isEqualTo(CustomAttributes.EMPTY);
        assertThat(pokemon.version()).isZero();
        assertThat(pokemon.syncedAt()).isEqualTo(T0);
        assertThat(repository.findByPokedexId(PIKACHU)).isPresent();
    }

    @Test
    void should_refresh_the_snapshot_and_report_not_created_on_resync() {
        useCase.sync(PIKACHU);
        upstream.with(CollectionFixtures.snapshot(25, "pikachu", 61));
        clock.advance(Duration.ofHours(1));

        SyncResult result = useCase.sync(PIKACHU);

        assertThat(result.created()).isFalse();
        assertThat(result.pokemon().snapshot().mass()).isEqualTo(new Mass(61));
        assertThat(result.pokemon().syncedAt()).isEqualTo(T0.plus(Duration.ofHours(1)));
    }

    @Test
    void should_preserve_custom_fields_when_resyncing() {
        CollectedPokemon created = useCase.sync(PIKACHU).pokemon();
        CustomAttributes custom = CustomAttributes.of(List.of(), null, List.of("starter"));
        created.updateCustom(custom, created.version(), clock);
        repository.save(created);

        SyncResult result = useCase.sync(PIKACHU);

        assertThat(result.pokemon().custom()).isEqualTo(custom);
        assertThat(repository.findByPokedexId(PIKACHU).orElseThrow().custom()).isEqualTo(custom);
    }

    @Test
    void should_keep_one_record_per_pokedex_id_however_often_it_is_synced() {
        CollectedPokemon first = useCase.sync(PIKACHU).pokemon();
        CollectedPokemon second = useCase.sync(PIKACHU).pokemon();
        CollectedPokemon third = useCase.sync(PIKACHU).pokemon();

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(third.id()).isEqualTo(first.id());
        assertThat(repository.findAll(CollectionQuery.firstPage()).totalElements())
                .isEqualTo(1);
    }

    @Test
    void should_fetch_the_snapshot_live_from_upstream_on_every_sync() {
        useCase.sync(PIKACHU);
        useCase.sync(PIKACHU);

        assertThat(upstream.calls()).containsExactly(PIKACHU, PIKACHU);
    }

    @Test
    void should_propagate_not_found_and_store_nothing_when_the_pokemon_is_not_upstream() {
        assertThatThrownBy(() -> useCase.sync(new PokedexId(99_999))).isInstanceOf(NotFoundException.class);

        assertThat(repository.saveCount()).isZero();
    }

    @Test
    void should_propagate_upstream_unavailable_and_leave_the_existing_record_untouched() {
        CollectedPokemon before = useCase.sync(PIKACHU).pokemon();
        UpstreamUnavailableException down = new UpstreamUnavailableException("down", new IOException("reset"));
        upstream.failingWith(down);

        assertThatThrownBy(() -> useCase.sync(PIKACHU)).isSameAs(down);

        assertThat(repository.findByPokedexId(PIKACHU).orElseThrow().snapshot()).isEqualTo(before.snapshot());
        assertThat(repository.saveCount()).isEqualTo(1);
    }

    @Test
    void should_reject_null_arguments() {
        assertThatThrownBy(() -> useCase.sync(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SyncPokemonService(null, repository, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SyncPokemonService(upstream, null, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SyncPokemonService(upstream, repository, (Clock) null))
                .isInstanceOf(NullPointerException.class);
    }
}
