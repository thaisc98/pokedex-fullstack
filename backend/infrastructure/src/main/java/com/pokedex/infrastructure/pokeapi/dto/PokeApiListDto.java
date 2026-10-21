package com.pokedex.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** {@code GET /pokemon?offset=&limit=}. {@code next} / {@code previous} are ignored, never followed. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiListDto(@Nullable Long count, @Nullable List<NamedResourceDto> results) {}
