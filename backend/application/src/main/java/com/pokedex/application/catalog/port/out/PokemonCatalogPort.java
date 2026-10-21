package com.pokedex.application.catalog.port.out;

import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;

/** Read-only source of the Pokémon catalog (PokeAPI behind an adapter and a cache decorator). */
public interface PokemonCatalogPort {

    /**
     * The complete page, or an exception: never a partially filled page.
     *
     * @throws com.pokedex.domain.shared.UpstreamUnavailableException when any part of the page cannot be
     *     fetched
     */
    Page<PokemonSummary> fetchPage(PageRequest request);
}
