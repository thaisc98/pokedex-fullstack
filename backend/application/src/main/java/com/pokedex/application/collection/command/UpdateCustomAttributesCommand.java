package com.pokedex.application.collection.command;

import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.CustomAttributes;
import com.pokedex.domain.shared.InvalidValueException;
import java.util.Objects;

/** UC-04: full replacement of the custom attributes, guarded by the version the client last saw (BR-08). */
public record UpdateCustomAttributesCommand(
        PokedexId pokedexId, CustomAttributes customAttributes, long expectedVersion) {

    public UpdateCustomAttributesCommand {
        Objects.requireNonNull(pokedexId, "pokedexId");
        Objects.requireNonNull(customAttributes, "customAttributes");
        if (expectedVersion < 0) {
            throw new InvalidValueException("version", "must be greater than or equal to 0");
        }
    }
}
