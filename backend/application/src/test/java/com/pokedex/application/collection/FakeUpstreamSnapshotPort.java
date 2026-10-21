package com.pokedex.application.collection;

import com.pokedex.application.collection.port.out.UpstreamSnapshotPort;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.UpstreamSnapshot;
import com.pokedex.domain.shared.NotFoundException;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * In-memory {@link UpstreamSnapshotPort} shipped in application's test-jar. Unknown ids throw
 * {@link NotFoundException}; {@link #failingWith} simulates PokeAPI being down. Records every call.
 */
public final class FakeUpstreamSnapshotPort implements UpstreamSnapshotPort {

    private final Map<PokedexId, UpstreamSnapshot> upstream = new HashMap<>();
    private final List<PokedexId> calls = new ArrayList<>();
    private @Nullable UpstreamUnavailableException failure;

    public FakeUpstreamSnapshotPort with(UpstreamSnapshot snapshot) {
        upstream.put(snapshot.id(), snapshot);
        return this;
    }

    public FakeUpstreamSnapshotPort failingWith(UpstreamUnavailableException failure) {
        this.failure = Objects.requireNonNull(failure);
        return this;
    }

    public List<PokedexId> calls() {
        return List.copyOf(calls);
    }

    @Override
    public UpstreamSnapshot fetchSnapshot(PokedexId id) {
        calls.add(id);
        if (failure != null) {
            throw failure;
        }
        UpstreamSnapshot snapshot = upstream.get(id);
        if (snapshot == null) {
            throw new NotFoundException("pokemon");
        }
        return snapshot;
    }
}
