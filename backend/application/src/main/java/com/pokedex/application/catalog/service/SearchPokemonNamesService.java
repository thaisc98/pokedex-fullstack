package com.pokedex.application.catalog.service;

import com.pokedex.application.catalog.port.in.SearchPokemonNamesUseCase;
import com.pokedex.application.catalog.port.out.PokemonCatalogPort;
import com.pokedex.domain.catalog.PokemonNameRef;
import com.pokedex.domain.catalog.vo.NameQuery;
import com.pokedex.domain.catalog.vo.SearchLimit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Name search over the catalog's (cached) name index, filtered in memory. Plain Java; a bean in bootstrap. */
public final class SearchPokemonNamesService implements SearchPokemonNamesUseCase {

    private static final Comparator<PokemonNameRef> BY_ID =
            Comparator.comparingInt(ref -> ref.id().value());

    private final PokemonCatalogPort catalog;

    public SearchPokemonNamesService(PokemonCatalogPort catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    @Override
    public List<PokemonNameRef> search(NameQuery query, SearchLimit limit) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(limit, "limit");
        List<PokemonNameRef> index = catalog.fetchNameIndex();
        Stream<PokemonNameRef> prefix =
                index.stream().filter(ref -> query.isPrefixOf(ref.name())).sorted(BY_ID);
        Stream<PokemonNameRef> contains = index.stream()
                .filter(ref -> !query.isPrefixOf(ref.name()) && query.isContainedIn(ref.name()))
                .sorted(BY_ID);
        return Stream.concat(prefix, contains).limit(limit.value()).toList();
    }
}
