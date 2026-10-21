package com.pokedex.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** The parts of {@code GET /pokemon-species/{id}} that US01/US02 need (genus, flavor text, evolution chain). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiSpeciesDto(
        @Nullable Integer id,
        @Nullable List<Genus> genera,
        @JsonProperty("flavor_text_entries") @Nullable List<FlavorText> flavorTextEntries,
        @JsonProperty("evolution_chain") @Nullable NamedResourceDto evolutionChain) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Genus(@Nullable String genus, @Nullable NamedResourceDto language) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FlavorText(
            @JsonProperty("flavor_text") @Nullable String text,
            @Nullable NamedResourceDto language,
            @Nullable NamedResourceDto version) {}
}
