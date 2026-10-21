package com.pokedex.presentation.rest.collection.dto;

import org.jspecify.annotations.Nullable;

/** openapi {@code LocalizedName}. */
public record LocalizedNameDto(
        @Nullable String lang, @Nullable String value) {}
