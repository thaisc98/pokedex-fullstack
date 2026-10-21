package com.pokedex.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** {@code GET /evolution-chain/{id}}: a recursive tree of chain links. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiEvolutionChainDto(
        @Nullable Integer id, @Nullable ChainLink chain) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChainLink(
            @Nullable NamedResourceDto species,
            @JsonProperty("evolution_details") @Nullable List<EvolutionDetail> evolutionDetails,
            @JsonProperty("evolves_to") @Nullable List<ChainLink> evolvesTo) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EvolutionDetail(
            @Nullable NamedResourceDto trigger,
            @JsonProperty("min_level") @Nullable Integer minLevel,
            @Nullable NamedResourceDto item,
            @JsonProperty("is_default") @Nullable Boolean isDefault) {}
}
