package com.pokedex.application.catalog;

import com.pokedex.application.catalog.port.out.PokemonCatalogPort;
import com.pokedex.domain.catalog.PokemonDetail;
import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.catalog.vo.PokemonRef;
import com.pokedex.domain.shared.NotFoundException;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * In-memory {@link PokemonCatalogPort} shipped in application's test-jar. It pages an in-memory list the way
 * PokeAPI pages its list endpoint, serves details by id or name (unknown → {@link NotFoundException}), records
 * every request, and can be switched into "upstream down" mode.
 */
public final class StubPokemonCatalog implements PokemonCatalogPort {

    private final List<PokemonSummary> catalog;
    private final List<PokemonDetail> details = new ArrayList<>();
    private final List<PageRequest> requests = new ArrayList<>();
    private final List<PokemonRef> detailRequests = new ArrayList<>();
    private @Nullable UpstreamUnavailableException failure;

    public StubPokemonCatalog(List<PokemonSummary> catalog) {
        this.catalog = List.copyOf(catalog);
    }

    /** A catalog with no list entries, only the given details. */
    public static StubPokemonCatalog withDetails(PokemonDetail... details) {
        StubPokemonCatalog stub = new StubPokemonCatalog(List.of());
        stub.details.addAll(List.of(details));
        return stub;
    }

    /** Every later call throws {@code failure}. */
    public StubPokemonCatalog failingWith(UpstreamUnavailableException failure) {
        this.failure = Objects.requireNonNull(failure);
        return this;
    }

    public List<PageRequest> requests() {
        return List.copyOf(requests);
    }

    public List<PokemonRef> detailRequests() {
        return List.copyOf(detailRequests);
    }

    @Override
    public Page<PokemonSummary> fetchPage(PageRequest request) {
        requests.add(request);
        if (failure != null) {
            throw failure;
        }
        long from = Math.min(request.offset(), catalog.size());
        long to = Math.min(from + request.size(), catalog.size());
        return new Page<>(catalog.subList((int) from, (int) to), request, catalog.size());
    }

    @Override
    public PokemonDetail fetchDetail(PokemonRef ref) {
        detailRequests.add(ref);
        if (failure != null) {
            throw failure;
        }
        return details.stream()
                .filter(detail -> switch (ref) {
                    case PokemonRef.ById byId -> detail.id().equals(byId.id());
                    case PokemonRef.ByName byName -> detail.name().equals(byName.name());
                })
                .findFirst()
                .orElseThrow(() -> new NotFoundException("pokemon"));
    }
}
