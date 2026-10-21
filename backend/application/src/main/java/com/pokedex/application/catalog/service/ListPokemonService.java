package com.pokedex.application.catalog.service;

import com.pokedex.application.catalog.port.in.ListPokemonUseCase;
import com.pokedex.application.catalog.port.out.PokemonCatalogPort;
import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import java.util.Objects;

/** UC-01. Plain Java; turned into a bean in bootstrap. */
public final class ListPokemonService implements ListPokemonUseCase {

    private final PokemonCatalogPort catalog;

    public ListPokemonService(PokemonCatalogPort catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    @Override
    public Page<PokemonSummary> list(PageRequest request) {
        Objects.requireNonNull(request, "request");
        return catalog.fetchPage(request);
    }
}
