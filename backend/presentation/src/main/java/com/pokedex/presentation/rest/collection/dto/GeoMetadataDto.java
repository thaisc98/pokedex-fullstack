package com.pokedex.presentation.rest.collection.dto;

import org.jspecify.annotations.Nullable;

/** openapi {@code GeoMetadata}: every field optional. */
public record GeoMetadataDto(
        @Nullable String region,
        @Nullable String habitat,
        @Nullable CoordinatesDto coordinates) {}
