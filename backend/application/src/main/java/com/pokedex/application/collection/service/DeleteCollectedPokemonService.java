package com.pokedex.application.collection.service;

import com.pokedex.application.collection.port.in.DeleteCollectedPokemonUseCase;
import com.pokedex.application.collection.port.out.CollectedPokemonRepository;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.identity.Role;
import com.pokedex.domain.shared.ForbiddenException;
import com.pokedex.domain.shared.NotFoundException;
import java.util.Objects;

/**
 * UC-05 delete. Checks the actor's role before anything else (defence in depth behind the filter chain), so
 * a non-ADMIN cannot even learn whether the record exists.
 */
public final class DeleteCollectedPokemonService implements DeleteCollectedPokemonUseCase {

    private final CollectedPokemonRepository repository;

    public DeleteCollectedPokemonService(CollectedPokemonRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public void delete(PokedexId id, Role actorRole) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(actorRole, "actorRole");
        if (actorRole != Role.ADMIN) {
            throw new ForbiddenException();
        }
        if (!repository.deleteByPokedexId(id)) {
            throw new NotFoundException(GetCollectedPokemonService.RESOURCE);
        }
    }
}
