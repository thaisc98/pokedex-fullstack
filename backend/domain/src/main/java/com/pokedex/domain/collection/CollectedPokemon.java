package com.pokedex.domain.collection;

import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.vo.CollectedPokemonId;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.domain.shared.VersionConflictException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root of the Collection: an upstream snapshot plus our own custom attributes.
 *
 * <ul>
 *   <li>BR-05: {@link #refreshFrom} replaces the snapshot and never touches the custom attributes.
 *   <li>BR-06: {@link #updateCustom} replaces only the custom attributes.
 *   <li>BR-08: {@link #updateCustom} rejects a version other than the current one. The repository, not
 *       the aggregate, increments the version when it stores a change.
 * </ul>
 */
public final class CollectedPokemon {

    private final CollectedPokemonId id;
    private UpstreamSnapshot snapshot;
    private CustomAttributes custom;
    private final long version;
    private Instant syncedAt;
    private Instant updatedAt;

    private CollectedPokemon(
            CollectedPokemonId id,
            UpstreamSnapshot snapshot,
            CustomAttributes custom,
            long version,
            Instant syncedAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.custom = Objects.requireNonNull(custom, "custom");
        if (version < 0) {
            throw new InvalidValueException("version", "must be greater than or equal to 0");
        }
        this.version = version;
        this.syncedAt = Objects.requireNonNull(syncedAt, "syncedAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** First sync: empty custom attributes, version 0, both timestamps now. */
    public static CollectedPokemon collect(CollectedPokemonId id, UpstreamSnapshot snapshot, Clock clock) {
        Instant now = clock.instant();
        return new CollectedPokemon(id, snapshot, CustomAttributes.EMPTY, 0, now, now);
    }

    /** Rebuilds a stored aggregate. Only persistence adapters call this. */
    public static CollectedPokemon rehydrate(
            CollectedPokemonId id,
            UpstreamSnapshot snapshot,
            CustomAttributes custom,
            long version,
            Instant syncedAt,
            Instant updatedAt) {
        return new CollectedPokemon(id, snapshot, custom, version, syncedAt, updatedAt);
    }

    public void refreshFrom(UpstreamSnapshot fresh, Clock clock) {
        Objects.requireNonNull(fresh, "fresh");
        if (!fresh.id().equals(snapshot.id())) {
            throw new InvalidValueException("pokedexId", "must match the collected Pokémon");
        }
        this.snapshot = fresh;
        this.syncedAt = clock.instant();
    }

    public void updateCustom(CustomAttributes replacement, long expectedVersion, Clock clock) {
        Objects.requireNonNull(replacement, "replacement");
        if (expectedVersion != version) {
            throw new VersionConflictException();
        }
        this.custom = replacement;
        this.updatedAt = clock.instant();
    }

    public CollectedPokemonId id() {
        return id;
    }

    public PokedexId pokedexId() {
        return snapshot.id();
    }

    public UpstreamSnapshot snapshot() {
        return snapshot;
    }

    public CustomAttributes custom() {
        return custom;
    }

    public long version() {
        return version;
    }

    public Instant syncedAt() {
        return syncedAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
