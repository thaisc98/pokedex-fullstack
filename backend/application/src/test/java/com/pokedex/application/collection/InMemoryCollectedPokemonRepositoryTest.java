package com.pokedex.application.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.collection.CustomAttributes;
import com.pokedex.domain.collection.vo.CollectedPokemonId;
import com.pokedex.domain.shared.VersionConflictException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins the {@code CollectedPokemonRepository} contract on the fake; the JPA adapter's tests in STEP 2 must
 * show the same behaviour (optimistic version, copies, one row per {@code PokedexId}).
 */
class InMemoryCollectedPokemonRepositoryTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);
    private static final PokedexId PIKACHU = new PokedexId(25);

    private final InMemoryCollectedPokemonRepository repository = new InMemoryCollectedPokemonRepository();

    @Test
    void should_insert_with_version_zero_and_increment_on_each_update() {
        CollectedPokemon inserted = repository.save(newPikachu());
        CollectedPokemon updated = repository.save(inserted);

        assertThat(inserted.version()).isZero();
        assertThat(updated.version()).isEqualTo(1);
    }

    @Test
    void should_reject_saving_a_stale_copy() {
        CollectedPokemon inserted = repository.save(newPikachu());
        CollectedPokemon copyA = repository.findByPokedexId(PIKACHU).orElseThrow();
        CollectedPokemon copyB = repository.findByPokedexId(PIKACHU).orElseThrow();
        repository.save(copyA);

        assertThatThrownBy(() -> repository.save(copyB)).isInstanceOf(VersionConflictException.class);
        assertThat(inserted.version()).isZero();
    }

    @Test
    void should_reject_a_second_aggregate_for_the_same_pokedex_id() {
        repository.save(newPikachu());

        assertThatThrownBy(() -> repository.save(newPikachu())).isInstanceOf(VersionConflictException.class);
    }

    @Test
    void should_not_leak_changes_made_to_a_loaded_aggregate_before_save() {
        repository.save(newPikachu());
        CollectedPokemon loaded = repository.findByPokedexId(PIKACHU).orElseThrow();

        loaded.updateCustom(CustomAttributes.of(List.of(), null, List.of("x")), 0, CLOCK);

        assertThat(repository.findByPokedexId(PIKACHU).orElseThrow().custom()).isEqualTo(CustomAttributes.EMPTY);
    }

    @Test
    void should_report_whether_a_delete_removed_a_row() {
        repository.save(newPikachu());

        assertThat(repository.deleteByPokedexId(PIKACHU)).isTrue();
        assertThat(repository.deleteByPokedexId(PIKACHU)).isFalse();
    }

    private static CollectedPokemon newPikachu() {
        return CollectedPokemon.collect(CollectedPokemonId.random(), CollectionFixtures.snapshot(25, "pikachu"), CLOCK);
    }
}
