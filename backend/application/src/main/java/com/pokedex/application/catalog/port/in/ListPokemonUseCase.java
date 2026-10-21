package com.pokedex.application.catalog.port.in;

import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;

/** UC-01 (US01): list Pokémon page by page. */
public interface ListPokemonUseCase {

    /**
     * @throws com.pokedex.domain.shared.UpstreamUnavailableException when the catalog cannot produce the
     *     complete page
     */
    Page<PokemonSummary> list(PageRequest request);
}
