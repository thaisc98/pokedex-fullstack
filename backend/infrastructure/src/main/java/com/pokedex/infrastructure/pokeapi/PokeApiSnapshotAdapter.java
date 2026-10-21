package com.pokedex.infrastructure.pokeapi;

import com.pokedex.application.collection.port.out.UpstreamSnapshotPort;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.UpstreamSnapshot;
import com.pokedex.domain.shared.NotFoundException;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Live {@link UpstreamSnapshotPort} for sync (UC-03): {@code /pokemon/{id}} then the species by the id in its
 * URL. Wired to the resilient gateway directly, never to the caching decorator, so a sync always copies the
 * current upstream data. Only a 404 on the Pokémon itself is {@link NotFoundException}.
 */
public final class PokeApiSnapshotAdapter implements UpstreamSnapshotPort {

    private static final Logger LOG = LoggerFactory.getLogger(PokeApiSnapshotAdapter.class);
    private static final String UNAVAILABLE = "PokeAPI is unavailable";

    private final PokeApiResources gateway;
    private final PokeApiMapper mapper;

    /** @param gateway the uncached, resilient gateway */
    public PokeApiSnapshotAdapter(PokeApiResources gateway, PokeApiMapper mapper) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public UpstreamSnapshot fetchSnapshot(PokedexId id) {
        PokeApiPokemonDto pokemon;
        try {
            pokemon = gateway.pokemon(id.value());
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("pokemon", e);
        } catch (RuntimeException e) {
            throw unavailable(e);
        }
        try {
            PokeApiSpeciesDto species = gateway.species(mapper.speciesId(pokemon));
            UpstreamSnapshot snapshot = mapper.toSnapshot(pokemon, species);
            if (!snapshot.id().equals(id)) {
                throw new IllegalStateException("PokeAPI answered for another Pokémon");
            }
            return snapshot;
        } catch (RuntimeException e) {
            throw unavailable(e);
        }
    }

    private static UpstreamUnavailableException unavailable(RuntimeException cause) {
        LOG.warn("PokeAPI snapshot failed: {}", cause.getClass().getName());
        return new UpstreamUnavailableException(UNAVAILABLE, cause);
    }
}
