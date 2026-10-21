package com.pokedex.domain.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.BaseStat;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.catalog.vo.StatName;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@code UpstreamSnapshot}: read-only copy of the PokeAPI data taken at sync time (BR-05, BR-06). */
class UpstreamSnapshotTest {

    private static final PokedexId ID = new PokedexId(25);
    private static final PokemonName NAME = new PokemonName("pikachu");
    private static final Mass MASS = new Mass(60);
    private static final List<Ability> NO_ABILITIES = List.of();
    private static final List<BaseStat> NO_STATS = List.of();

    @Test
    void should_keep_every_upstream_field() {
        UpstreamSnapshot snapshot = SnapshotFixtures.pikachu();

        assertThat(snapshot.id()).isEqualTo(ID);
        assertThat(snapshot.name()).isEqualTo(NAME);
        assertThat(snapshot.sprite()).isEqualTo(ImageUrl.PLACEHOLDER);
        assertThat(snapshot.category()).isEqualTo(new Category("Mouse Pokémon"));
        assertThat(snapshot.mass()).isEqualTo(MASS);
        assertThat(snapshot.abilities()).extracting(Ability::name).containsExactly("static", "lightning-rod");
        assertThat(snapshot.stats()).isEqualTo(SnapshotFixtures.STATS);
    }

    @Test
    void should_copy_the_abilities_defensively() {
        List<Ability> abilities = new ArrayList<>(List.of(new Ability("static", false)));
        UpstreamSnapshot snapshot =
                new UpstreamSnapshot(ID, NAME, ImageUrl.PLACEHOLDER, Category.UNKNOWN, MASS, abilities, NO_STATS);

        abilities.add(new Ability("lightning-rod", true));

        assertThat(snapshot.abilities()).hasSize(1);
        assertThatThrownBy(() -> snapshot.abilities().add(new Ability("x", false)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_copy_the_stats_defensively() {
        List<BaseStat> stats = new ArrayList<>(List.of(new BaseStat(StatName.HP, 35)));
        UpstreamSnapshot snapshot =
                new UpstreamSnapshot(ID, NAME, ImageUrl.PLACEHOLDER, Category.UNKNOWN, MASS, NO_ABILITIES, stats);

        stats.add(new BaseStat(StatName.SPEED, 90));

        assertThat(snapshot.stats()).hasSize(1);
        assertThatThrownBy(() -> snapshot.stats().add(new BaseStat(StatName.ATTACK, 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_reject_null_fields() {
        ImageUrl sprite = ImageUrl.PLACEHOLDER;
        Category cat = Category.UNKNOWN;

        assertNpe(() -> new UpstreamSnapshot(null, NAME, sprite, cat, MASS, NO_ABILITIES, NO_STATS));
        assertNpe(() -> new UpstreamSnapshot(ID, null, sprite, cat, MASS, NO_ABILITIES, NO_STATS));
        assertNpe(() -> new UpstreamSnapshot(ID, NAME, null, cat, MASS, NO_ABILITIES, NO_STATS));
        assertNpe(() -> new UpstreamSnapshot(ID, NAME, sprite, null, MASS, NO_ABILITIES, NO_STATS));
        assertNpe(() -> new UpstreamSnapshot(ID, NAME, sprite, cat, null, NO_ABILITIES, NO_STATS));
        assertNpe(() -> new UpstreamSnapshot(ID, NAME, sprite, cat, MASS, null, NO_STATS));
        assertNpe(() -> new UpstreamSnapshot(ID, NAME, sprite, cat, MASS, NO_ABILITIES, null));
    }

    private static void assertNpe(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOf(NullPointerException.class);
    }
}
