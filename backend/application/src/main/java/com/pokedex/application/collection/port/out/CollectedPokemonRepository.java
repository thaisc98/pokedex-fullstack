package com.pokedex.application.collection.port.out;

import com.pokedex.application.collection.command.CollectionQuery;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.shared.Page;
import java.util.Optional;

/**
 * Storage for collected Pokémon, one row per {@code PokedexId}, with optimistic versioning (BR-08).
 *
 * <ul>
 *   <li>{@link #save} inserts when no row exists for the {@code PokedexId}, keeping the aggregate's version.
 *   <li>Otherwise it updates only when the stored id and version match the aggregate's, and returns the
 *       stored state with {@code version + 1}.
 *   <li>Anything else (stale version, or a concurrent insert for the same {@code PokedexId}) throws
 *       {@link com.pokedex.domain.shared.VersionConflictException}.
 *   <li>{@link #findAll} orders by {@code PokedexId} ascending.
 * </ul>
 */
public interface CollectedPokemonRepository {

    Optional<CollectedPokemon> findByPokedexId(PokedexId id);

    CollectedPokemon save(CollectedPokemon pokemon);

    Page<CollectedPokemon> findAll(CollectionQuery query);

    /** @return whether a row was removed */
    boolean deleteByPokedexId(PokedexId id);
}
