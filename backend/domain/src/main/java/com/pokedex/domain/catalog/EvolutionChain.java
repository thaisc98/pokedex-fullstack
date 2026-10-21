package com.pokedex.domain.catalog;

import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.shared.InvalidValueException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * The full evolution tree of a species family (BR-04): branches kept, stable order, rooted at a stage with no
 * trigger. Each species appears once.
 */
public record EvolutionChain(EvolutionNode root) {

    public EvolutionChain {
        Objects.requireNonNull(root, "root");
        if (root.trigger() != null) {
            throw new InvalidValueException("evolution", "the first stage has no trigger");
        }
        List<PokedexId> ids = collect(root, new ArrayList<>());
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new InvalidValueException("evolution", "each species may appear only once");
        }
    }

    /** Every species in the tree, depth-first in upstream order. */
    public List<PokedexId> speciesIds() {
        return List.copyOf(collect(root, new ArrayList<>()));
    }

    public boolean contains(PokedexId speciesId) {
        return speciesIds().contains(speciesId);
    }

    private static List<PokedexId> collect(EvolutionNode node, List<PokedexId> into) {
        into.add(node.speciesId());
        node.evolvesTo().forEach(next -> collect(next, into));
        return into;
    }
}
