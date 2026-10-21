package com.pokedex.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** The parts of {@code GET /pokemon-species/{id}} that US01 needs (the genus). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiSpeciesDto(
        @Nullable Integer id, @Nullable List<Genus> genera) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Genus(@Nullable String genus, @Nullable NamedResourceDto language) {}
}
