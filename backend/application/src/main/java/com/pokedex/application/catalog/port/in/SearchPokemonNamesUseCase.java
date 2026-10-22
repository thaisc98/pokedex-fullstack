package com.pokedex.application.catalog.port.in;

import com.pokedex.domain.catalog.PokemonNameRef;
import com.pokedex.domain.catalog.vo.NameQuery;
import com.pokedex.domain.catalog.vo.SearchLimit;
import java.util.List;

/** Pokémon name search (autocomplete). */
public interface SearchPokemonNamesUseCase {

    /**
     * Names starting with {@code query} first, then names containing it, each group by Pokédex id, at most
     * {@code limit}. No match is an empty list.
     *
     * @throws com.pokedex.domain.shared.UpstreamUnavailableException when the name index cannot be loaded
     */
    List<PokemonNameRef> search(NameQuery query, SearchLimit limit);
}
