package com.pokedex.application.collection.service;

import com.pokedex.application.collection.port.in.GetCollectedPokemonUseCase;
import com.pokedex.application.collection.port.out.CollectedPokemonRepository;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.shared.NotFoundException;
import java.util.Objects;

/** UC-05 get by Pokédex id; missing → {@link NotFoundException}. */
public final class GetCollectedPokemonService implements GetCollectedPokemonUseCase {

    static final String RESOURCE = "collected pokemon";

    private final CollectedPokemonRepository repository;

    public GetCollectedPokemonService(CollectedPokemonRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public CollectedPokemon get(PokedexId id) {
        Objects.requireNonNull(id, "id");
        return repository.findByPokedexId(id).orElseThrow(() -> new NotFoundException(RESOURCE));
    }
}
