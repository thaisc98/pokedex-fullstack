package com.pokedex.infrastructure.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.BaseStat;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.catalog.vo.StatName;
import com.pokedex.domain.collection.UpstreamSnapshot;
import com.pokedex.domain.shared.NotFoundException;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Live {@code UpstreamSnapshotPort} for sync (UC-03): {@code /pokemon/{id}} plus {@code /pokemon-species/{id}},
 * read straight from the gateway (no cache), with the same failure mapping as the catalog adapter.
 */
class PokeApiSnapshotAdapterTest {

    private final FakePokeApiHttpClient client = new FakePokeApiHttpClient();
    private final PokeApiSnapshotAdapter adapter =
            new PokeApiSnapshotAdapter(new PokeApiGateway(client), new PokeApiMapper());

    @Test
    void should_build_the_snapshot_from_pokemon_and_species() {
        client.pokemon(25, () -> PokeApiFixtures.pokemon(25)).species(25, () -> PokeApiFixtures.species(25));

        UpstreamSnapshot snapshot = adapter.fetchSnapshot(new PokedexId(25));

        assertThat(snapshot.id()).isEqualTo(new PokedexId(25));
        assertThat(snapshot.name()).isEqualTo(new PokemonName("pikachu"));
        assertThat(snapshot.category()).isEqualTo(new Category("Mouse Pokémon"));
        assertThat(snapshot.mass()).isEqualTo(new Mass(60));
        assertThat(snapshot.sprite().value()).startsWith("https://raw.githubusercontent.com/PokeAPI/sprites/");
        assertThat(snapshot.abilities())
                .containsExactly(new Ability("static", false), new Ability("lightning-rod", true));
        assertThat(snapshot.stats()).hasSize(6).contains(new BaseStat(StatName.HP, 35));
    }

    @Test
    void should_call_upstream_on_every_fetch() {
        client.pokemon(25, () -> PokeApiFixtures.pokemon(25)).species(25, () -> PokeApiFixtures.species(25));

        adapter.fetchSnapshot(new PokedexId(25));
        adapter.fetchSnapshot(new PokedexId(25));

        assertThat(client.calls())
                .containsExactly("pokemon/25", "pokemon-species/25", "pokemon/25", "pokemon-species/25");
    }

    @Test
    void should_report_not_found_when_the_pokemon_is_not_upstream() {
        assertThatThrownBy(() -> adapter.fetchSnapshot(new PokedexId(99_999)))
                .isInstanceOfSatisfying(
                        NotFoundException.class, e -> assertThat(e.resource()).isEqualTo("pokemon"));
    }

    @Test
    void should_report_unavailable_when_the_species_is_missing() {
        client.pokemon(25, () -> PokeApiFixtures.pokemon(25));

        assertThatThrownBy(() -> adapter.fetchSnapshot(new PokedexId(25)))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void should_report_unavailable_when_upstream_fails() {
        client.pokemon(25, () -> {
            throw new IllegalStateException("boom");
        });

        assertThatThrownBy(() -> adapter.fetchSnapshot(new PokedexId(25)))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void should_report_unavailable_when_upstream_answers_for_another_pokemon() {
        client.pokemon(26, () -> PokeApiFixtures.pokemon(25)).species(25, () -> PokeApiFixtures.species(25));

        assertThatThrownBy(() -> adapter.fetchSnapshot(new PokedexId(26)))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    // ------------------------------------------------------------ a live re-sync refreshes the catalog caches

    @Test
    void should_hand_the_fresh_pokemon_and_species_to_the_listener_after_a_successful_fetch() {
        List<String> refreshed = new ArrayList<>();
        PokeApiSnapshotAdapter listening = new PokeApiSnapshotAdapter(
                new PokeApiGateway(client),
                new PokeApiMapper(),
                (pokemon, species) -> refreshed.add(pokemon.name() + "/" + species.id()));
        client.pokemon(25, () -> PokeApiFixtures.pokemon(25)).species(25, () -> PokeApiFixtures.species(25));

        listening.fetchSnapshot(new PokedexId(25));

        assertThat(refreshed).containsExactly("pikachu/25");
    }

    @Test
    void should_not_notify_the_listener_when_the_fetch_fails() {
        List<String> refreshed = new ArrayList<>();
        PokeApiSnapshotAdapter listening = new PokeApiSnapshotAdapter(
                new PokeApiGateway(client), new PokeApiMapper(), (pokemon, species) -> refreshed.add("called"));
        client.pokemon(26, () -> PokeApiFixtures.pokemon(25)).species(25, () -> PokeApiFixtures.species(25));

        assertThatThrownBy(() -> listening.fetchSnapshot(new PokedexId(99_999))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> listening.fetchSnapshot(new PokedexId(26)))
                .isInstanceOf(UpstreamUnavailableException.class);

        assertThat(refreshed).isEmpty();
    }
}
