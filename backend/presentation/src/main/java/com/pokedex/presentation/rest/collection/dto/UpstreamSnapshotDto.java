package com.pokedex.presentation.rest.collection.dto;

import com.pokedex.presentation.rest.catalog.dto.AbilityDto;
import com.pokedex.presentation.rest.catalog.dto.BaseStatDto;
import java.util.List;

/**
 * openapi {@code UpstreamSnapshot} (additionalProperties: false).
 *
 * @param mass kilograms rounded to 0.1 (BR-02)
 */
public record UpstreamSnapshotDto(
        int id,
        String name,
        String sprite,
        String category,
        double mass,
        List<AbilityDto> abilities,
        List<BaseStatDto> stats) {

    public UpstreamSnapshotDto {
        abilities = List.copyOf(abilities);
        stats = List.copyOf(stats);
    }
}
