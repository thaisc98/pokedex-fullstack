package com.pokedex.application.catalog.port.out;

import com.pokedex.domain.catalog.PokemonDetail;
import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.catalog.vo.PokemonRef;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;

/** Read-only source of the Pokémon catalog (PokeAPI behind an adapter and cache decorators). */
public interface PokemonCatalogPort {

    /**
     * The complete page, or an exception: never a partially filled page.
     *
     * @throws com.pokedex.domain.shared.UpstreamUnavailableException when any part of the page cannot be
     *     fetched
     */
    Page<PokemonSummary> fetchPage(PageRequest request);

    /**
     * The complete detail of one Pokémon: its resource, species and evolution chain.
     *
     * @throws com.pokedex.domain.shared.NotFoundException when the catalog has no such Pokémon
     * @throws com.pokedex.domain.shared.UpstreamUnavailableException when any part cannot be fetched
     */
    PokemonDetail fetchDetail(PokemonRef ref);
}
