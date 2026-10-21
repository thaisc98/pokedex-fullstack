package com.pokedex.infrastructure.pokeapi;

import com.pokedex.application.catalog.port.out.PokemonCatalogPort;
import com.pokedex.domain.catalog.PokemonDetail;
import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.catalog.vo.PokemonRef;
import com.pokedex.domain.shared.NotFoundException;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import com.pokedex.infrastructure.pokeapi.dto.NamedResourceDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiEvolutionChainDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiListDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

/**
 * {@link PokemonCatalogPort} on PokeAPI (architecture.md §1.5): one list call, then for every result its
 * {@code /pokemon/{id}} and {@code /pokemon-species/{id}} calls, the items in parallel on virtual threads.
 * Ids are extracted from upstream URLs and the requests rebuilt against the configured base; upstream URLs
 * are never followed. Any failure fails the whole page with {@link UpstreamUnavailableException}: no
 * half-filled cards.
 */
public final class PokeApiCatalogAdapter implements PokemonCatalogPort {

    private static final Logger LOG = LoggerFactory.getLogger(PokeApiCatalogAdapter.class);
    private static final String UNAVAILABLE = "PokeAPI is unavailable";

    private final PokeApiResources gateway;
    private final PokeApiMapper mapper;

    /**
     * @param gateway the PokeAPI resources, normally the per-resource cache in front of the resilient gateway
     */
    public PokeApiCatalogAdapter(PokeApiResources gateway, PokeApiMapper mapper) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    /**
     * {@code /pokemon/{id or name}}, then the species and the evolution chain by the ids in their URLs. Only a
     * PokeAPI 404 on the Pokémon itself means {@link NotFoundException}; anything else failing (a 404 on the
     * species or chain included, which would mean inconsistent upstream data) is {@link
     * UpstreamUnavailableException}.
     */
    @Override
    public PokemonDetail fetchDetail(PokemonRef ref) {
        PokeApiPokemonDto pokemon;
        try {
            pokemon = switch (ref) {
                case PokemonRef.ById byId -> gateway.pokemon(byId.id().value());
                case PokemonRef.ByName byName ->
                    gateway.pokemonByName(byName.name().value());
            };
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("pokemon", e);
        } catch (RuntimeException e) {
            throw unavailable("detail", e);
        }
        try {
            PokeApiSpeciesDto species = gateway.species(mapper.speciesId(pokemon));
            PokeApiEvolutionChainDto chain = gateway.evolutionChain(mapper.evolutionChainId(species));
            return mapper.toDetail(pokemon, species, chain);
        } catch (RuntimeException e) {
            throw unavailable("detail", e);
        }
    }

    private static UpstreamUnavailableException unavailable(String what, RuntimeException cause) {
        LOG.warn("PokeAPI {} failed: {}", what, cause.getClass().getName());
        return new UpstreamUnavailableException(UNAVAILABLE, cause);
    }

    @Override
    public Page<PokemonSummary> fetchPage(PageRequest request) {
        try {
            PokeApiListDto list = gateway.list(request.offset(), request.size());
            List<NamedResourceDto> results = Objects.requireNonNull(list.results(), "results");
            if (results.size() > request.size()) {
                throw new IllegalStateException("PokeAPI returned more results than requested");
            }
            List<Integer> ids = results.stream()
                    .map(result -> mapper.pokemonId(
                            Objects.requireNonNull(result, "result").url()))
                    .toList();
            return new Page<>(fetchAll(ids), request, Objects.requireNonNull(list.count(), "count"));
        } catch (RuntimeException e) {
            LOG.warn(
                    "PokeAPI page {} (size {}) failed: {}",
                    request.page(),
                    request.size(),
                    e.getClass().getName());
            throw new UpstreamUnavailableException(UNAVAILABLE, e);
        }
    }

    private List<PokemonSummary> fetchAll(List<Integer> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<PokemonSummary>> futures = new ArrayList<>(ids.size());
            for (int id : ids) {
                futures.add(executor.submit(() -> fetchOne(id)));
            }
            List<PokemonSummary> summaries = new ArrayList<>(ids.size());
            try {
                for (Future<PokemonSummary> future : futures) {
                    summaries.add(future.get());
                }
            } catch (ExecutionException | InterruptedException | RuntimeException e) {
                futures.forEach(future -> future.cancel(true));
                throw asRuntime(e);
            }
            return summaries;
        }
    }

    private PokemonSummary fetchOne(int id) {
        PokeApiPokemonDto pokemon = gateway.pokemon(id);
        return mapper.toSummary(pokemon, gateway.species(mapper.speciesId(pokemon)));
    }

    private static RuntimeException asRuntime(Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return new IllegalStateException("Interrupted while fetching PokeAPI resources", e);
        }
        Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
        if (cause instanceof RuntimeException runtime) {
            return runtime;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        return new IllegalStateException("PokeAPI resource fetch failed", cause);
    }
}
