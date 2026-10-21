package com.pokedex.presentation.rest.collection.dto;

import org.jspecify.annotations.Nullable;

/** openapi {@code SyncRequest}. {@code pokedexId} is nullable here so a missing value is a 400, not a 500. */
public record SyncRequest(@Nullable Integer pokedexId) {}
