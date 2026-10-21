package com.pokedex.application.collection.service;

import com.pokedex.application.collection.command.CollectionQuery;
import com.pokedex.application.collection.port.in.ListCollectedPokemonUseCase;
import com.pokedex.application.collection.port.out.CollectedPokemonRepository;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.shared.Page;
import java.util.Objects;

/** UC-05 list. The collection is a shared team dataset (security.md §3.1), so there is no owner filter. */
public final class ListCollectedPokemonService implements ListCollectedPokemonUseCase {

    private final CollectedPokemonRepository repository;

    public ListCollectedPokemonService(CollectedPokemonRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public Page<CollectedPokemon> list(CollectionQuery query) {
        Objects.requireNonNull(query, "query");
        return repository.findAll(query);
    }
}
