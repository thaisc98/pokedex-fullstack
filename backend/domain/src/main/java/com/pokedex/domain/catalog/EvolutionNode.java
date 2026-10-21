package com.pokedex.domain.catalog;

import com.pokedex.domain.catalog.vo.EvolutionTrigger;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.shared.InvalidValueException;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A stage in an evolution tree (BR-04). {@code evolvesTo} keeps upstream order and may branch; every next stage
 * carries the trigger that reaches it. Only a chain root has no trigger (checked by {@link EvolutionChain}).
 */
public record EvolutionNode(
        PokedexId speciesId, PokemonName name, @Nullable EvolutionTrigger trigger, List<EvolutionNode> evolvesTo) {

    public EvolutionNode {
        Objects.requireNonNull(speciesId, "speciesId");
        Objects.requireNonNull(name, "name");
        evolvesTo = List.copyOf(Objects.requireNonNull(evolvesTo, "evolvesTo"));
        if (evolvesTo.stream().anyMatch(next -> next.trigger() == null)) {
            throw new InvalidValueException("evolution", "every evolution stage after the first needs a trigger");
        }
    }
}
