package com.pokedex.infrastructure.persistence;

import com.pokedex.application.collection.command.CollectionQuery;
import com.pokedex.application.collection.port.out.CollectedPokemonRepository;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.VersionConflictException;
import com.pokedex.infrastructure.persistence.entity.CollectedPokemonEntity;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link CollectedPokemonRepository} on JPA. Optimistic concurrency (BR-08) has two layers: the stored
 * version is compared with the aggregate's before writing, and {@code @Version} catches a concurrent commit
 * between read and flush. A lost insert race (unique {@code pokedex_id}) is also a version conflict, which
 * sync turns into one retry as a refresh.
 */
public final class JpaCollectedPokemonRepositoryAdapter implements CollectedPokemonRepository {

    static final String POKEDEX_ID_UNIQUE = "uk_collected_pokemon_pokedex_id";
    private static final Sort BY_POKEDEX_ID = Sort.by("pokedexId");

    private final SpringDataCollectedPokemonRepository rows;
    private final CollectedPokemonEntityMapper mapper;
    private final TransactionTemplate transactions;

    public JpaCollectedPokemonRepositoryAdapter(
            SpringDataCollectedPokemonRepository rows,
            CollectedPokemonEntityMapper mapper,
            TransactionTemplate transactions) {
        this.rows = Objects.requireNonNull(rows, "rows");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    @Override
    public Optional<CollectedPokemon> findByPokedexId(PokedexId id) {
        return rows.findByPokedexId(id.value()).map(mapper::toDomain);
    }

    @Override
    public CollectedPokemon save(CollectedPokemon pokemon) {
        try {
            return Objects.requireNonNull(transactions.execute(status -> write(pokemon)));
        } catch (OptimisticLockingFailureException e) {
            throw new VersionConflictException();
        } catch (DataIntegrityViolationException e) {
            if (POKEDEX_ID_UNIQUE.equalsIgnoreCase(constraintName(e))) {
                throw new VersionConflictException();
            }
            throw e;
        }
    }

    private CollectedPokemon write(CollectedPokemon pokemon) {
        Optional<CollectedPokemonEntity> stored =
                rows.findByPokedexId(pokemon.pokedexId().value());
        if (stored.isEmpty()) {
            return mapper.toDomain(rows.saveAndFlush(mapper.toNewEntity(pokemon)));
        }
        CollectedPokemonEntity entity = stored.get();
        if (!entity.getId().equals(pokemon.id().value()) || !Objects.equals(entity.getVersion(), pokemon.version())) {
            throw new VersionConflictException();
        }
        mapper.copyInto(pokemon, entity);
        return mapper.toDomain(rows.saveAndFlush(entity));
    }

    /** Two queries whatever the page size: one page of rows (plus its count), then those rows with collections. */
    @Override
    public Page<CollectedPokemon> findAll(CollectionQuery query) {
        org.springframework.data.domain.Page<CollectedPokemonEntity> page = rows.findAll(
                CollectionSpecifications.matching(query.tag(), query.name()),
                org.springframework.data.domain.PageRequest.of(
                        query.page().page(), query.page().size(), BY_POKEDEX_ID));
        List<UUID> ids =
                page.getContent().stream().map(CollectedPokemonEntity::getId).toList();
        List<CollectedPokemon> content = ids.isEmpty()
                ? List.of()
                : rows.findAllByIdIn(ids, BY_POKEDEX_ID).stream()
                        .map(mapper::toDomain)
                        .toList();
        return new Page<>(content, query.page(), page.getTotalElements());
    }

    @Override
    public boolean deleteByPokedexId(PokedexId id) {
        Long removed = transactions.execute(status -> rows.deleteByPokedexId(id.value()));
        return removed != null && removed > 0;
    }

    private static @Nullable String constraintName(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }
        return null;
    }
}
