package com.pokedex.presentation.rest.collection.dto;

import java.time.Instant;

/**
 * openapi {@code CollectedPokemonDto} (additionalProperties: false). {@code id} is the Pokédex id; the internal UUID
 * is never exposed (security.md §4.3).
 */
public record CollectedPokemonDto(
        int id,
        UpstreamSnapshotDto snapshot,
        CustomAttributesDto custom,
        long version,
        Instant syncedAt,
        Instant updatedAt) {}
