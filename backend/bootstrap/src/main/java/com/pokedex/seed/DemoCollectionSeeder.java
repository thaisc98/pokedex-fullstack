package com.pokedex.seed;

import com.pokedex.application.collection.command.UpdateCustomAttributesCommand;
import com.pokedex.application.collection.port.in.SyncPokemonUseCase;
import com.pokedex.application.collection.port.in.UpdateCollectedPokemonUseCase;
import com.pokedex.application.collection.port.out.CollectedPokemonRepository;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.collection.CustomAttributes;
import com.pokedex.domain.collection.vo.GeoMetadata;
import com.pokedex.domain.collection.vo.LocalizedName;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demo profile only: a small Gen-1 collection synced from PokeAPI (UC-03), then given custom attributes (UC-04).
 * Idempotent (collected ids are skipped). Never fatal: if PokeAPI is unreachable it logs one WARN and stops.
 */
public final class DemoCollectionSeeder {

    /** One Pokémon to collect and the custom attributes to give it. */
    public record Entry(int pokedexId, CustomAttributes custom) {}

    private static final String KANTO = "Kanto";

    public static final List<Entry> GEN_ONE = List.of(
            entry(1, "フシギダネ", "grassland", "starter", "grass"),
            entry(4, "ヒトカゲ", "mountain", "starter", "fire"),
            entry(7, "ゼニガメ", "waters-edge", "starter", "water"),
            entry(25, "ピカチュウ", "forest", "mascot", "electric"),
            entry(133, "イーブイ", "urban", "evolves", "normal"),
            entry(143, "カビゴン", "mountain", "sleepy", "normal"));

    private static final Logger LOG = LoggerFactory.getLogger(DemoCollectionSeeder.class);

    private final CollectedPokemonRepository collection;
    private final SyncPokemonUseCase sync;
    private final UpdateCollectedPokemonUseCase update;
    private final List<Entry> entries;

    public DemoCollectionSeeder(
            CollectedPokemonRepository collection,
            SyncPokemonUseCase sync,
            UpdateCollectedPokemonUseCase update,
            List<Entry> entries) {
        this.collection = Objects.requireNonNull(collection, "collection");
        this.sync = Objects.requireNonNull(sync, "sync");
        this.update = Objects.requireNonNull(update, "update");
        this.entries = List.copyOf(entries);
    }

    /** @return how many Pokémon were added */
    public int seed() {
        int added = 0;
        for (Entry entry : entries) {
            PokedexId id = new PokedexId(entry.pokedexId());
            if (collection.findByPokedexId(id).isPresent()) {
                continue;
            }
            try {
                CollectedPokemon synced = sync.sync(id).pokemon();
                update.update(new UpdateCustomAttributesCommand(id, entry.custom(), synced.version()));
                added++;
            } catch (UpstreamUnavailableException e) {
                LOG.warn("Demo seed: PokeAPI is unavailable; skipping the rest of the demo collection");
                break;
            }
        }
        if (added > 0) {
            LOG.info("Demo seed: added {} Pokémon to the collection", added);
        }
        return added;
    }

    private static Entry entry(int id, String japaneseName, String habitat, String... tags) {
        return new Entry(
                id,
                CustomAttributes.of(
                        List.of(new LocalizedName("ja", japaneseName)),
                        new GeoMetadata(KANTO, habitat, null),
                        List.of(tags)));
    }
}
