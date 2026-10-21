package com.pokedex.domain.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.vo.CollectedPokemonId;
import com.pokedex.domain.collection.vo.LocalizedName;
import com.pokedex.domain.collection.vo.Tag;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.domain.shared.VersionConflictException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@code CollectedPokemon} aggregate: sync creates or refreshes the snapshot without touching custom
 * attributes (BR-05); US04 changes only custom attributes (BR-06) and checks the version (BR-08).
 */
class CollectedPokemonTest {

    private static final Instant T0 = Instant.parse("2026-10-21T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-10-21T11:00:00Z");
    private static final Clock AT_T0 = Clock.fixed(T0, ZoneOffset.UTC);
    private static final Clock AT_T1 = Clock.fixed(T1, ZoneOffset.UTC);
    private static final CustomAttributes CUSTOM =
            CustomAttributes.of(List.of(new LocalizedName("ja", "ピカチュウ")), null, List.of("starter"));

    @Test
    void should_start_with_empty_custom_attributes_version_zero_and_clock_timestamps() {
        CollectedPokemonId id = CollectedPokemonId.random();

        CollectedPokemon pokemon = CollectedPokemon.collect(id, SnapshotFixtures.pikachu(), AT_T0);

        assertThat(pokemon.id()).isEqualTo(id);
        assertThat(pokemon.pokedexId()).isEqualTo(new PokedexId(25));
        assertThat(pokemon.snapshot()).isEqualTo(SnapshotFixtures.pikachu());
        assertThat(pokemon.custom()).isEqualTo(CustomAttributes.EMPTY);
        assertThat(pokemon.version()).isZero();
        assertThat(pokemon.syncedAt()).isEqualTo(T0);
        assertThat(pokemon.updatedAt()).isEqualTo(T0);
    }

    @Test
    void should_replace_the_snapshot_and_sync_time_on_refresh() {
        CollectedPokemon pokemon =
                CollectedPokemon.collect(CollectedPokemonId.random(), SnapshotFixtures.pikachu(), AT_T0);
        UpstreamSnapshot newer = SnapshotFixtures.snapshot(25, "pikachu", 61);

        pokemon.refreshFrom(newer, AT_T1);

        assertThat(pokemon.snapshot().mass()).isEqualTo(new Mass(61));
        assertThat(pokemon.syncedAt()).isEqualTo(T1);
    }

    @Test
    void should_preserve_custom_fields_when_resyncing() {
        CollectedPokemon pokemon = existing(CUSTOM, 3);

        pokemon.refreshFrom(SnapshotFixtures.snapshot(25, "pikachu", 61), AT_T1);

        assertThat(pokemon.custom()).isEqualTo(CUSTOM);
        assertThat(pokemon.updatedAt()).isEqualTo(T0);
        assertThat(pokemon.version()).isEqualTo(3);
    }

    @Test
    void should_reject_a_snapshot_of_a_different_pokemon() {
        CollectedPokemon pokemon = existing(CUSTOM, 0);

        assertThatThrownBy(() -> pokemon.refreshFrom(SnapshotFixtures.snapshot(26, "raichu", 300), AT_T1))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("pokedexId"));
        assertThat(pokemon.snapshot()).isEqualTo(SnapshotFixtures.pikachu());
    }

    @Test
    void should_replace_custom_attributes_and_update_time_when_the_version_matches() {
        CollectedPokemon pokemon = existing(CustomAttributes.EMPTY, 2);

        pokemon.updateCustom(CUSTOM, 2, AT_T1);

        assertThat(pokemon.custom()).isEqualTo(CUSTOM);
        assertThat(pokemon.updatedAt()).isEqualTo(T1);
        assertThat(pokemon.syncedAt()).isEqualTo(T0);
        assertThat(pokemon.snapshot()).isEqualTo(SnapshotFixtures.pikachu());
    }

    @Test
    void should_replace_not_merge_custom_attributes() {
        CollectedPokemon pokemon = existing(CUSTOM, 0);
        CustomAttributes onlyTag = new CustomAttributes(Set.of(), null, Set.of(new Tag("electric")));

        pokemon.updateCustom(onlyTag, 0, AT_T1);

        assertThat(pokemon.custom().localizedNames()).isEmpty();
        assertThat(pokemon.custom().tags()).containsExactly(new Tag("electric"));
    }

    @Test
    void should_leave_the_version_to_the_repository() {
        CollectedPokemon pokemon = existing(CustomAttributes.EMPTY, 2);

        pokemon.updateCustom(CUSTOM, 2, AT_T1);

        assertThat(pokemon.version()).isEqualTo(2);
    }

    @Test
    void should_reject_a_stale_version_and_change_nothing() {
        CollectedPokemon pokemon = existing(CUSTOM, 5);

        assertThatThrownBy(() -> pokemon.updateCustom(CustomAttributes.EMPTY, 4, AT_T1))
                .isInstanceOf(VersionConflictException.class);
        assertThat(pokemon.custom()).isEqualTo(CUSTOM);
        assertThat(pokemon.updatedAt()).isEqualTo(T0);
    }

    @Test
    void should_reject_a_version_from_the_future() {
        CollectedPokemon pokemon = existing(CUSTOM, 5);

        assertThatThrownBy(() -> pokemon.updateCustom(CustomAttributes.EMPTY, 6, AT_T1))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void should_rehydrate_every_field_from_storage() {
        CollectedPokemonId id = CollectedPokemonId.random();

        CollectedPokemon pokemon = CollectedPokemon.rehydrate(id, SnapshotFixtures.pikachu(), CUSTOM, 7, T0, T1);

        assertThat(pokemon.id()).isEqualTo(id);
        assertThat(pokemon.custom()).isEqualTo(CUSTOM);
        assertThat(pokemon.version()).isEqualTo(7);
        assertThat(pokemon.syncedAt()).isEqualTo(T0);
        assertThat(pokemon.updatedAt()).isEqualTo(T1);
    }

    @Test
    void should_reject_a_negative_version_on_rehydrate() {
        assertThatThrownBy(() -> CollectedPokemon.rehydrate(
                        CollectedPokemonId.random(), SnapshotFixtures.pikachu(), CUSTOM, -1, T0, T0))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("version"));
    }

    @Test
    void should_reject_nulls() {
        CollectedPokemon pokemon = existing(CUSTOM, 0);

        assertThatThrownBy(() -> CollectedPokemon.collect(null, SnapshotFixtures.pikachu(), AT_T0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> CollectedPokemon.collect(CollectedPokemonId.random(), null, AT_T0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> pokemon.refreshFrom(null, AT_T1)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> pokemon.updateCustom(null, 0, AT_T1)).isInstanceOf(NullPointerException.class);
    }

    private static CollectedPokemon existing(CustomAttributes custom, long version) {
        return CollectedPokemon.rehydrate(
                CollectedPokemonId.random(), SnapshotFixtures.pikachu(), custom, version, T0, T0);
    }
}
