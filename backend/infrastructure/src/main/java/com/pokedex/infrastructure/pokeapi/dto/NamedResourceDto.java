package com.pokedex.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.Nullable;

/** PokeAPI {@code NamedAPIResource}: {@code {name, url}}. The URL is never followed. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NamedResourceDto(
        @Nullable String name, @Nullable String url) {}
