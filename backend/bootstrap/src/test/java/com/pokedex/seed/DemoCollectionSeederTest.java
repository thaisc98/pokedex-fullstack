package com.pokedex.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokedex.application.collection.CollectionFixtures;
import com.pokedex.application.collection.FakeUpstreamSnapshotPort;
import com.pokedex.application.collection.InMemoryCollectedPokemonRepository;
import com.pokedex.application.collection.service.SyncPokemonService;
import com.pokedex.application.collection.service.UpdateCollectedPokemonService;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.collection.vo.Tag;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Demo collection: a few Gen-1 Pokémon synced through UC-03, then given custom attributes through UC-04.
 * Idempotent (ids already collected are skipped) and never fatal: if PokeAPI is down it logs one WARN and stops.
 */
@ExtendWith(OutputCaptureExtension.class)
class DemoCollectionSeederTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);
    private final InMemoryCollectedPokemonRepository repository = new InMemoryCollectedPokemonRepository();
    private final FakeUpstreamSnapshotPort upstream = new FakeUpstreamSnapshotPort();

    private DemoCollectionSeeder seeder() {
        return new DemoCollectionSeeder(
                repository,
                new SyncPokemonService(upstream, repository, clock),
                new UpdateCollectedPokemonService(repository, clock),
                DemoCollectionSeeder.GEN_ONE);
    }

    private void upstreamKnowsAll() {
        for (DemoCollectionSeeder.Entry entry : DemoCollectionSeeder.GEN_ONE) {
            upstream.with(CollectionFixtures.snapshot(entry.pokedexId(), "pokemon-" + entry.pokedexId()));
        }
    }

    @Test
    void should_sync_every_entry_with_its_custom_attributes() {
        upstreamKnowsAll();

        assertThat(seeder().seed()).isEqualTo(DemoCollectionSeeder.GEN_ONE.size());

        CollectedPokemon bulbasaur =
                repository.findByPokedexId(new PokedexId(1)).orElseThrow();
        assertThat(bulbasaur.custom().tags()).contains(new Tag("starter"));
        assertThat(bulbasaur.custom().localizedNames()).isNotEmpty();
        assertThat(bulbasaur.custom().geo()).isNotNull();
    }

    @Test
    void should_skip_pokemon_already_in_the_collection() {
        upstreamKnowsAll();
        seeder().seed();
        int callsAfterFirstRun = upstream.calls().size();

        assertThat(seeder().seed()).isZero();
        assertThat(upstream.calls()).as("no PokeAPI call on the second start").hasSize(callsAfterFirstRun);
    }

    @Test
    void should_log_one_warn_and_stop_when_pokeapi_is_down(CapturedOutput output) {
        upstream.failingWith(new UpstreamUnavailableException("down", new IOException("refused")));

        assertThat(seeder().seed()).isZero();

        assertThat(upstream.calls()).hasSize(1);
        assertThat(output.getAll().lines().filter(line -> line.contains("WARN")))
                .hasSize(1);
    }
}
