package com.pokedex.presentation.rest.collection.dto;

import org.jspecify.annotations.Nullable;

/** openapi {@code Coordinates}: both required when present. */
public record CoordinatesDto(
        @Nullable Double latitude, @Nullable Double longitude) {}
