package com.pokedex.application.collection.service;

import com.pokedex.application.collection.command.UpdateCustomAttributesCommand;
import com.pokedex.application.collection.port.in.UpdateCollectedPokemonUseCase;
import com.pokedex.application.collection.port.out.CollectedPokemonRepository;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.shared.NotFoundException;
import java.time.Clock;
import java.util.Objects;

/**
 * UC-04. The aggregate checks the expected version; the repository checks it again on save, which catches
 * a concurrent update between load and save (BR-08).
 */
public final class UpdateCollectedPokemonService implements UpdateCollectedPokemonUseCase {

    private final CollectedPokemonRepository repository;
    private final Clock clock;

    public UpdateCollectedPokemonService(CollectedPokemonRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public CollectedPokemon update(UpdateCustomAttributesCommand command) {
        Objects.requireNonNull(command, "command");
        CollectedPokemon pokemon = repository
                .findByPokedexId(command.pokedexId())
                .orElseThrow(() -> new NotFoundException(GetCollectedPokemonService.RESOURCE));
        pokemon.updateCustom(command.customAttributes(), command.expectedVersion(), clock);
        return repository.save(pokemon);
    }
}
