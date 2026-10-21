package com.pokedex.presentation.rest.catalog;

import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.shared.Page;
import com.pokedex.presentation.rest.catalog.dto.AbilityDto;
import com.pokedex.presentation.rest.catalog.dto.PokemonSummaryDto;
import com.pokedex.presentation.rest.shared.PageResponse;

/** Hand-written domain → REST DTO mapping. Domain objects never leave the presentation layer. */
final class CatalogDtoMapper {

    private CatalogDtoMapper() {}

    static PokemonSummaryDto toDto(PokemonSummary summary) {
        return new PokemonSummaryDto(
                summary.id().value(),
                summary.name().value(),
                summary.sprite().value(),
                summary.category().value(),
                summary.mass().kilograms(),
                summary.abilities().stream()
                        .map(ability -> new AbilityDto(ability.name(), ability.hidden()))
                        .toList());
    }

    static PageResponse<PokemonSummaryDto> toResponse(Page<PokemonSummary> page) {
        return new PageResponse<>(
                page.content().stream().map(CatalogDtoMapper::toDto).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
