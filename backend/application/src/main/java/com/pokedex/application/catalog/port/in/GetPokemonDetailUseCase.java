package com.pokedex.application.catalog.port.in;

import com.pokedex.domain.catalog.PokemonDetail;
import com.pokedex.domain.catalog.vo.PokemonRef;

/** UC-02 (US02): the detail of one Pokémon, by Pokédex id or name. */
public interface GetPokemonDetailUseCase {

    /**
     * @throws com.pokedex.domain.shared.NotFoundException when no such Pokémon exists
     * @throws com.pokedex.domain.shared.UpstreamUnavailableException when the catalog cannot answer completely
     */
    PokemonDetail get(PokemonRef ref);
}
