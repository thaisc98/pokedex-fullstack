package com.pokedex.presentation.rest.catalog.dto;

import java.util.List;

/**
 * openapi {@code PokemonSummaryDto} (additionalProperties: false): exactly these fields.
 *
 * @param mass kilograms rounded to 0.1 (BR-02)
 */
public record PokemonSummaryDto(
        int id, String name, String sprite, String category, double mass, List<AbilityDto> abilities) {

    public PokemonSummaryDto {
        abilities = List.copyOf(abilities);
    }
}
