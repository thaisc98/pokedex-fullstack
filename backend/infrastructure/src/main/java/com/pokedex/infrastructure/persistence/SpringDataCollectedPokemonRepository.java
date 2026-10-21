package com.pokedex.infrastructure.persistence;

import com.pokedex.infrastructure.persistence.entity.CollectedPokemonEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Spring Data access to {@code collected_pokemon}. Reads that map to the domain load both collections. */
public interface SpringDataCollectedPokemonRepository
        extends JpaRepository<CollectedPokemonEntity, UUID>, JpaSpecificationExecutor<CollectedPokemonEntity> {

    @EntityGraph(attributePaths = {"tags", "localizedNames"})
    Optional<CollectedPokemonEntity> findByPokedexId(int pokedexId);

    @EntityGraph(attributePaths = {"tags", "localizedNames"})
    List<CollectedPokemonEntity> findAllByIdIn(Collection<UUID> ids, Sort sort);

    long deleteByPokedexId(int pokedexId);
}
