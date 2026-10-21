package com.pokedex.presentation.rest.catalog.dto;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * openapi {@code EvolutionNode}: {@code {speciesId, name, trigger, evolvesTo}}. {@code trigger} is {@code null} on
 * the chain root (the contract makes it nullable), so it is always written.
 */
public record EvolutionNodeDto(
        int speciesId, String name, @Nullable EvolutionTriggerDto trigger, List<EvolutionNodeDto> evolvesTo) {

    public EvolutionNodeDto {
        evolvesTo = List.copyOf(evolvesTo);
    }
}
