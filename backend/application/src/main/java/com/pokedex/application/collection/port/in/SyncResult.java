package com.pokedex.application.collection.port.in;

import com.pokedex.domain.collection.CollectedPokemon;
import java.util.Objects;

/** UC-03 outcome: {@code created} is true for a first sync (201) and false for a refresh (200). */
public record SyncResult(CollectedPokemon pokemon, boolean created) {

    public SyncResult {
        Objects.requireNonNull(pokemon, "pokemon");
    }
}
