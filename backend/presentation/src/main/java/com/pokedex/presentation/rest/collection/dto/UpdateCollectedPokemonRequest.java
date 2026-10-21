package com.pokedex.presentation.rest.collection.dto;

import org.jspecify.annotations.Nullable;

/**
 * openapi {@code UpdateCollectedPokemonRequest}: only {@code customAttributes} and {@code version}. Any other field
 * (an upstream one such as {@code name}, BR-06) is unknown and rejected with 400.
 */
public record UpdateCollectedPokemonRequest(
        @Nullable CustomAttributesDto customAttributes,
        @Nullable Long version) {}
