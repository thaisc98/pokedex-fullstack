package com.pokedex.domain.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@code UpstreamSnapshot}: read-only copy of the PokeAPI data taken at sync time (BR-05, BR-06). */
class UpstreamSnapshotTest {

    @Test
    void should_keep_every_upstream_field() {
        UpstreamSnapshot snapshot = SnapshotFixtures.pikachu();

        assertThat(snapshot.id()).isEqualTo(new PokedexId(25));
        assertThat(snapshot.name()).isEqualTo(new PokemonName("pikachu"));
        assertThat(snapshot.sprite()).isEqualTo(ImageUrl.PLACEHOLDER);
        assertThat(snapshot.category()).isEqualTo(new Category("Mouse Pokémon"));
        assertThat(snapshot.mass()).isEqualTo(new Mass(60));
        assertThat(snapshot.abilities()).extracting(Ability::name).containsExactly("static", "lightning-rod");
    }

    @Test
    void should_copy_the_abilities_defensively() {
        List<Ability> abilities = new ArrayList<>(List.of(new Ability("static", false)));
        UpstreamSnapshot snapshot = new UpstreamSnapshot(
                new PokedexId(25),
                new PokemonName("pikachu"),
                ImageUrl.PLACEHOLDER,
                Category.UNKNOWN,
                new Mass(60),
                abilities);

        abilities.add(new Ability("lightning-rod", true));

        assertThat(snapshot.abilities()).hasSize(1);
        assertThatThrownBy(() -> snapshot.abilities().add(new Ability("x", false)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_reject_null_fields() {
        PokedexId id = new PokedexId(25);
        PokemonName name = new PokemonName("pikachu");
        Mass mass = new Mass(60);
        List<Ability> none = List.of();

        assertThatThrownBy(() -> new UpstreamSnapshot(null, name, ImageUrl.PLACEHOLDER, Category.UNKNOWN, mass, none))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpstreamSnapshot(id, null, ImageUrl.PLACEHOLDER, Category.UNKNOWN, mass, none))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpstreamSnapshot(id, name, null, Category.UNKNOWN, mass, none))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpstreamSnapshot(id, name, ImageUrl.PLACEHOLDER, null, mass, none))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpstreamSnapshot(id, name, ImageUrl.PLACEHOLDER, Category.UNKNOWN, null, none))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpstreamSnapshot(id, name, ImageUrl.PLACEHOLDER, Category.UNKNOWN, mass, null))
                .isInstanceOf(NullPointerException.class);
    }
}
