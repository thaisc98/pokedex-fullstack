package com.pokedex.application.collection;

import com.pokedex.application.collection.command.CollectionQuery;
import com.pokedex.application.collection.port.out.CollectedPokemonRepository;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.VersionConflictException;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * In-memory {@link CollectedPokemonRepository} shipped in application's test-jar. Mirrors the semantics
 * the JPA adapter must honour:
 *
 * <ul>
 *   <li>Stores and returns copies, so a loaded aggregate changes nothing until {@link #save}.
 *   <li>Insert: no row for the {@code PokedexId}; stored with the aggregate's version (0).
 *   <li>Update: same {@code CollectedPokemonId} and stored version equal to the aggregate's version;
 *       the stored and returned copy has {@code version + 1} (JPA {@code @Version}).
 *   <li>Anything else (stale version, or a lost insert race for the same {@code PokedexId}) throws
 *       {@link VersionConflictException}.
 *   <li>{@code findAll} orders by {@code PokedexId} ascending; {@code tag} is exact, {@code name} is a
 *       case-insensitive literal substring of the snapshot name.
 * </ul>
 */
public final class InMemoryCollectedPokemonRepository implements CollectedPokemonRepository {

    private final TreeMap<Integer, CollectedPokemon> rows = new TreeMap<>();
    private final Deque<Runnable> beforeSave = new ArrayDeque<>();
    private int failingSaves;
    private int saves;

    public int saveCount() {
        return saves;
    }

    /** Runs {@code action} at the start of the next {@link #save}, e.g. a concurrent request's insert. */
    public InMemoryCollectedPokemonRepository beforeNextSave(Runnable action) {
        beforeSave.add(Objects.requireNonNull(action));
        return this;
    }

    /** The next {@code count} saves throw {@link VersionConflictException} without storing anything. */
    public InMemoryCollectedPokemonRepository failNextSaves(int count) {
        failingSaves = count;
        return this;
    }

    @Override
    public Optional<CollectedPokemon> findByPokedexId(PokedexId id) {
        return Optional.ofNullable(rows.get(id.value())).map(InMemoryCollectedPokemonRepository::copy);
    }

    @Override
    public CollectedPokemon save(CollectedPokemon pokemon) {
        Runnable hook = beforeSave.poll();
        if (hook != null) {
            hook.run();
        }
        if (failingSaves > 0) {
            failingSaves--;
            throw new VersionConflictException();
        }
        CollectedPokemon stored = rows.get(pokemon.pokedexId().value());
        CollectedPokemon toStore;
        if (stored == null) {
            toStore = copy(pokemon, pokemon.version());
        } else if (stored.id().equals(pokemon.id()) && stored.version() == pokemon.version()) {
            toStore = copy(pokemon, pokemon.version() + 1);
        } else {
            throw new VersionConflictException();
        }
        rows.put(pokemon.pokedexId().value(), toStore);
        saves++;
        return copy(toStore);
    }

    @Override
    public Page<CollectedPokemon> findAll(CollectionQuery query) {
        List<CollectedPokemon> matching = rows.values().stream()
                .filter(p -> query.tag() == null || p.custom().tags().contains(query.tag()))
                .filter(p -> query.name() == null || p.snapshot().name().value().contains(query.name()))
                .sorted(Comparator.comparingInt(p -> p.pokedexId().value()))
                .toList();
        long from = Math.min(query.page().offset(), matching.size());
        long to = Math.min(from + query.page().size(), matching.size());
        List<CollectedPokemon> content = matching.subList((int) from, (int) to).stream()
                .map(InMemoryCollectedPokemonRepository::copy)
                .toList();
        return new Page<>(content, query.page(), matching.size());
    }

    @Override
    public boolean deleteByPokedexId(PokedexId id) {
        return rows.remove(id.value()) != null;
    }

    private static CollectedPokemon copy(CollectedPokemon p) {
        return copy(p, p.version());
    }

    private static CollectedPokemon copy(CollectedPokemon p, long version) {
        return CollectedPokemon.rehydrate(p.id(), p.snapshot(), p.custom(), version, p.syncedAt(), p.updatedAt());
    }
}
