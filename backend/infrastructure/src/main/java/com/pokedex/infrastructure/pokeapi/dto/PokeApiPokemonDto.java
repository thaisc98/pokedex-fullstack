package com.pokedex.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** The parts of {@code GET /pokemon/{id}} that US01/US02 need. Everything else (moves, cries…) is ignored. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiPokemonDto(
        @Nullable Integer id,
        @Nullable String name,
        @Nullable Integer weight,
        @Nullable Integer height,
        @Nullable Sprites sprites,
        @Nullable List<AbilitySlot> abilities,
        @Nullable List<StatSlot> stats,
        @Nullable List<TypeSlot> types,
        @Nullable NamedResourceDto species) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sprites(
            @JsonProperty("front_default") @Nullable String frontDefault,
            @Nullable OtherSprites other) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OtherSprites(
            @JsonProperty("official-artwork") @Nullable Artwork officialArtwork) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Artwork(
            @JsonProperty("front_default") @Nullable String frontDefault) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AbilitySlot(
            @Nullable NamedResourceDto ability,
            @JsonProperty("is_hidden") boolean hidden) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatSlot(
            @JsonProperty("base_stat") @Nullable Integer baseStat,
            @Nullable NamedResourceDto stat) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TypeSlot(@Nullable NamedResourceDto type) {}
}
