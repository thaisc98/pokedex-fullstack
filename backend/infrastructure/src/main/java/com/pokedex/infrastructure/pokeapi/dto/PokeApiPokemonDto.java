package com.pokedex.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** The parts of {@code GET /pokemon/{id}} that US01 needs. Everything else (moves, stats…) is ignored. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiPokemonDto(
        @Nullable Integer id,
        @Nullable String name,
        @Nullable Integer weight,
        @Nullable Sprites sprites,
        @Nullable List<AbilitySlot> abilities,
        @Nullable NamedResourceDto species) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sprites(
            @JsonProperty("front_default") @Nullable String frontDefault) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AbilitySlot(
            @Nullable NamedResourceDto ability,
            @JsonProperty("is_hidden") boolean hidden) {}
}
