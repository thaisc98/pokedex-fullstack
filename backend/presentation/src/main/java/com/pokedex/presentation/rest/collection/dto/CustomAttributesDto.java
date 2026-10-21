package com.pokedex.presentation.rest.collection.dto;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** openapi {@code CustomAttributes}, in requests and responses. Lists are never null in a response. */
public record CustomAttributesDto(
        @Nullable List<@Nullable LocalizedNameDto> localizedNames,
        @Nullable GeoMetadataDto geo,
        @Nullable List<@Nullable String> tags) {}
