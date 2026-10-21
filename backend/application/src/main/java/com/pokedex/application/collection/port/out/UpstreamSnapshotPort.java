package com.pokedex.application.collection.port.out;

import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.UpstreamSnapshot;

/**
 * Live read of one Pokémon from the upstream catalog, used by sync. Implementations must not go through
 * the catalog cache: a sync always copies the current upstream data.
 */
public interface UpstreamSnapshotPort {

    /**
     * @throws com.pokedex.domain.shared.NotFoundException if the Pokémon does not exist upstream
     * @throws com.pokedex.domain.shared.UpstreamUnavailableException if upstream cannot answer completely
     */
    UpstreamSnapshot fetchSnapshot(PokedexId id);
}
