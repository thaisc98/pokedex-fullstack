package com.pokedex.application.catalog.port.out;

import com.pokedex.domain.catalog.PokemonDetail;
import com.pokedex.domain.catalog.PokemonNameRef;
import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.catalog.vo.PokemonRef;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import java.util.List;

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

    /**
     * Every Pokémon's id and name, for the name search (one upstream list call, cached).
     *
     * @throws com.pokedex.domain.shared.UpstreamUnavailableException when the index cannot be loaded
     */
    List<PokemonNameRef> fetchNameIndex();
}
