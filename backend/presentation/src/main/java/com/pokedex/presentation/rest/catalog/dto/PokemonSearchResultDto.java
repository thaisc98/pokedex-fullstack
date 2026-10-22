package com.pokedex.presentation.rest.catalog.dto;

import java.util.List;

/** openapi {@code PokemonSearchResult}: {@code {results: [{id, name}]}}. */
public record PokemonSearchResultDto(List<PokemonNameRefDto> results) {

    public PokemonSearchResultDto {
        results = List.copyOf(results);
    }
}
