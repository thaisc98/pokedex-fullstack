package com.pokedex.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** One row of {@code collected_pokemon_localized_name}; at most one per language (primary key). */
@Embeddable
public record LocalizedNameEmbeddable(
        @Column(name = "lang", nullable = false, length = 35)
        String lang,

        @Column(name = "value", nullable = false, length = 60)
        String value) {}
