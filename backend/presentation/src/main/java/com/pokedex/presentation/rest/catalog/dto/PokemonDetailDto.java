package com.pokedex.presentation.rest.catalog.dto;

import java.util.List;

/**
 * openapi {@code PokemonDetailDto} (additionalProperties: false): exactly these fields.
 *
 * @param types type slugs (openapi {@code PokemonType})
 * @param height meters
 * @param mass kilograms rounded to 0.1 (BR-02)
 */
public record PokemonDetailDto(
        int id,
        String name,
        String artwork,
        List<BaseStatDto> stats,
        String description,
        EvolutionChainDto evolution,
        List<String> types,
        double height,
        double mass) {

    public PokemonDetailDto {
        stats = List.copyOf(stats);
        types = List.copyOf(types);
    }
}
