package com.pokedex.application.catalog.service;

import com.pokedex.application.catalog.port.in.GetPokemonDetailUseCase;
import com.pokedex.application.catalog.port.out.PokemonCatalogPort;
import com.pokedex.domain.catalog.PokemonDetail;
import com.pokedex.domain.catalog.vo.PokemonRef;
import java.util.Objects;

/** UC-02. Plain Java; turned into a bean in bootstrap. */
public final class GetPokemonDetailService implements GetPokemonDetailUseCase {

    private final PokemonCatalogPort catalog;

    public GetPokemonDetailService(PokemonCatalogPort catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    @Override
    public PokemonDetail get(PokemonRef ref) {
        Objects.requireNonNull(ref, "ref");
        return catalog.fetchDetail(ref);
    }
}
