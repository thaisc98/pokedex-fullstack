package com.pokedex.application.collection.service;

import com.pokedex.application.collection.port.in.SyncPokemonUseCase;
import com.pokedex.application.collection.port.in.SyncResult;
import com.pokedex.application.collection.port.out.CollectedPokemonRepository;
import com.pokedex.application.collection.port.out.UpstreamSnapshotPort;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.collection.UpstreamSnapshot;
import com.pokedex.domain.collection.vo.CollectedPokemonId;
import com.pokedex.domain.shared.VersionConflictException;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/**
 * UC-03. Fetches the snapshot live, then inserts or refreshes (BR-05). A concurrent sync of the same
 * Pokémon makes the save fail with a version conflict; the service then retries once as a refresh of the
 * winner's row, so two simultaneous first syncs give one row and one "created".
 */
public final class SyncPokemonService implements SyncPokemonUseCase {

    private final UpstreamSnapshotPort upstream;
    private final CollectedPokemonRepository repository;
    private final Clock clock;

    public SyncPokemonService(UpstreamSnapshotPort upstream, CollectedPokemonRepository repository, Clock clock) {
        this.upstream = Objects.requireNonNull(upstream, "upstream");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public SyncResult sync(PokedexId id) {
        Objects.requireNonNull(id, "id");
        UpstreamSnapshot snapshot = upstream.fetchSnapshot(id);
        try {
            return upsert(snapshot);
        } catch (VersionConflictException concurrentWrite) {
            return upsert(snapshot);
        }
    }

    private SyncResult upsert(UpstreamSnapshot snapshot) {
        Optional<CollectedPokemon> existing = repository.findByPokedexId(snapshot.id());
        if (existing.isPresent()) {
            CollectedPokemon pokemon = existing.get();
            pokemon.refreshFrom(snapshot, clock);
            return new SyncResult(repository.save(pokemon), false);
        }
        CollectedPokemon created = CollectedPokemon.collect(CollectedPokemonId.random(), snapshot, clock);
        return new SyncResult(repository.save(created), true);
    }
}
