package com.pokedex.infrastructure.persistence;

import static com.pokedex.infrastructure.persistence.CollectedPokemonFixtures.FULL_CUSTOM;
import static com.pokedex.infrastructure.persistence.CollectedPokemonFixtures.pikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pokedex.application.collection.command.CollectionQuery;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.collection.CustomAttributes;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.domain.shared.VersionConflictException;
import com.pokedex.infrastructure.persistence.entity.CollectedPokemonEntity;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code CollectedPokemonRepository} adapter: the port contract (insert / version-checked update / conflict
 * translation / two-step paging without N+1). The SQL itself is covered by the PostgreSQL ITs.
 */
class JpaCollectedPokemonRepositoryAdapterTest {

    private final SpringDataCollectedPokemonRepository springData = mock(SpringDataCollectedPokemonRepository.class);
    private final CollectedPokemonEntityMapper mapper = new CollectedPokemonEntityMapper();
    private final JpaCollectedPokemonRepositoryAdapter adapter = new JpaCollectedPokemonRepositoryAdapter(
            springData, mapper, new TransactionTemplate(mock(PlatformTransactionManager.class)));

    @Test
    void should_insert_when_no_row_exists() {
        CollectedPokemon pokemon = pikachu(CustomAttributes.EMPTY, 0);
        when(springData.findByPokedexId(25)).thenReturn(Optional.empty());
        when(springData.saveAndFlush(any())).thenAnswer(call -> versioned(call.getArgument(0), 0L));

        CollectedPokemon saved = adapter.save(pokemon);

        assertThat(saved.id()).isEqualTo(pokemon.id());
        assertThat(saved.version()).isZero();
    }

    @Test
    void should_update_the_managed_entity_when_id_and_version_match() {
        CollectedPokemon stored = pikachu(CustomAttributes.EMPTY, 2);
        CollectedPokemonEntity managed = versioned(mapper.toNewEntity(stored), 2L);
        when(springData.findByPokedexId(25)).thenReturn(Optional.of(managed));
        when(springData.saveAndFlush(managed)).thenAnswer(call -> versioned(managed, 3L));
        CollectedPokemon changed = CollectedPokemon.rehydrate(
                stored.id(), stored.snapshot(), FULL_CUSTOM, 2, stored.syncedAt(), stored.updatedAt());

        CollectedPokemon saved = adapter.save(changed);

        assertThat(saved.version()).isEqualTo(3);
        assertThat(saved.custom()).isEqualTo(FULL_CUSTOM);
    }

    @Test
    void should_reject_a_stale_version_without_writing() {
        CollectedPokemon stored = pikachu(CustomAttributes.EMPTY, 2);
        when(springData.findByPokedexId(25)).thenReturn(Optional.of(versioned(mapper.toNewEntity(stored), 3L)));

        assertThatThrownBy(() -> adapter.save(stored)).isInstanceOf(VersionConflictException.class);

        verify(springData, never()).saveAndFlush(any());
    }

    @Test
    void should_reject_another_aggregate_for_the_same_pokedex_id() {
        when(springData.findByPokedexId(25))
                .thenReturn(Optional.of(versioned(mapper.toNewEntity(pikachu(CustomAttributes.EMPTY, 0)), 0L)));

        assertThatThrownBy(() -> adapter.save(pikachu(CustomAttributes.EMPTY, 0)))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void should_translate_a_concurrent_insert_into_a_version_conflict() {
        when(springData.findByPokedexId(25)).thenReturn(Optional.empty());
        when(springData.saveAndFlush(any()))
                .thenThrow(uniqueViolation(JpaCollectedPokemonRepositoryAdapter.POKEDEX_ID_UNIQUE));

        assertThatThrownBy(() -> adapter.save(pikachu(CustomAttributes.EMPTY, 0)))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void should_rethrow_other_integrity_violations() {
        when(springData.findByPokedexId(25)).thenReturn(Optional.empty());
        DataIntegrityViolationException other = uniqueViolation("ck_something_else");
        when(springData.saveAndFlush(any())).thenThrow(other);

        assertThatThrownBy(() -> adapter.save(pikachu(CustomAttributes.EMPTY, 0)))
                .isSameAs(other);
    }

    @Test
    void should_translate_an_optimistic_lock_failure_into_a_version_conflict() {
        CollectedPokemon stored = pikachu(CustomAttributes.EMPTY, 1);
        CollectedPokemonEntity managed = versioned(mapper.toNewEntity(stored), 1L);
        when(springData.findByPokedexId(25)).thenReturn(Optional.of(managed));
        when(springData.saveAndFlush(managed))
                .thenThrow(new ObjectOptimisticLockingFailureException(
                        CollectedPokemonEntity.class, stored.id().value()));

        assertThatThrownBy(() -> adapter.save(stored)).isInstanceOf(VersionConflictException.class);
    }

    @Test
    void should_find_by_pokedex_id() {
        CollectedPokemon stored = pikachu(FULL_CUSTOM, 0);
        when(springData.findByPokedexId(25)).thenReturn(Optional.of(versioned(mapper.toNewEntity(stored), 0L)));

        assertThat(adapter.findByPokedexId(new PokedexId(25)))
                .get()
                .extracting(CollectedPokemon::id)
                .isEqualTo(stored.id());
        assertThat(adapter.findByPokedexId(new PokedexId(26))).isEmpty();
    }

    @Test
    void should_page_ids_first_then_fetch_the_rows_with_their_collections_in_one_query() {
        CollectedPokemonEntity first = versioned(mapper.toNewEntity(pikachu(FULL_CUSTOM, 0)), 0L);
        when(springData.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first), org.springframework.data.domain.PageRequest.of(1, 1), 3));
        when(springData.findAllByIdIn(anyCollection(), any(Sort.class))).thenReturn(List.of(first));

        Page<CollectedPokemon> page = adapter.findAll(CollectionQuery.of(new PageRequest(1, 1), null, null));

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.page()).isEqualTo(1);
        verify(springData)
                .findAll(
                        any(Specification.class),
                        org.mockito.ArgumentMatchers.eq(
                                org.springframework.data.domain.PageRequest.of(1, 1, Sort.by("pokedexId"))));
    }

    @Test
    void should_skip_the_fetch_query_for_an_empty_page() {
        when(springData.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(5, 20), 2));

        Page<CollectedPokemon> page = adapter.findAll(CollectionQuery.of(new PageRequest(5, 20), null, null));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(2);
        verify(springData, never()).findAllByIdIn(anyCollection(), any(Sort.class));
    }

    @Test
    void should_report_whether_delete_removed_a_row() {
        when(springData.deleteByPokedexId(25)).thenReturn(1L);
        when(springData.deleteByPokedexId(26)).thenReturn(0L);

        assertThat(adapter.deleteByPokedexId(new PokedexId(25))).isTrue();
        assertThat(adapter.deleteByPokedexId(new PokedexId(26))).isFalse();
    }

    private static CollectedPokemonEntity versioned(CollectedPokemonEntity entity, Long version) {
        entity.setVersion(version);
        return entity;
    }

    private static DataIntegrityViolationException uniqueViolation(String constraint) {
        return new DataIntegrityViolationException(
                "duplicate", new ConstraintViolationException("duplicate", new SQLException("23505"), constraint));
    }
}
