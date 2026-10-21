package com.pokedex.application.collection.port.in;

import com.pokedex.application.collection.command.UpdateCustomAttributesCommand;
import com.pokedex.domain.collection.CollectedPokemon;

/** UC-04: replace the custom attributes (BR-06, BR-08). */
public interface UpdateCollectedPokemonUseCase {

    CollectedPokemon update(UpdateCustomAttributesCommand command);
}
