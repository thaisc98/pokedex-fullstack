package com.pokedex.infrastructure.persistence;

import static com.pokedex.infrastructure.persistence.CollectedPokemonFixtures.FULL_CUSTOM;
import static com.pokedex.infrastructure.persistence.CollectedPokemonFixtures.pikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.collection.CustomAttributes;
import com.pokedex.domain.collection.vo.Coordinates;
import com.pokedex.domain.collection.vo.GeoMetadata;
import com.pokedex.infrastructure.persistence.entity.CollectedPokemonEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** {@code CollectedPokemon} ↔ {@code CollectedPokemonEntity} by hand; entities never leave infrastructure. */
class CollectedPokemonEntityMapperTest {

    private final CollectedPokemonEntityMapper mapper = new CollectedPokemonEntityMapper();

    @Test
    void should_round_trip_every_field() {
        CollectedPokemon pokemon = pikachu(FULL_CUSTOM, 0);

        CollectedPokemon back = mapper.toDomain(withVersion(mapper.toNewEntity(pokemon), 0L));

        assertSameState(back, pokemon);
    }

    @Test
    void should_store_the_snapshot_columns_and_json() {
        CollectedPokemonEntity entity = mapper.toNewEntity(pikachu(FULL_CUSTOM, 0));

        assertThat(entity.getPokedexId()).isEqualTo(25);
        assertThat(entity.getName()).isEqualTo("pikachu");
        assertThat(entity.getCategory()).isEqualTo("Mouse Pokémon");
        assertThat(entity.getWeightHg()).isEqualTo(60);
        assertThat(entity.getAbilitiesJson())
                .isEqualTo("[{\"name\":\"static\",\"hidden\":false},{\"name\":\"lightning-rod\",\"hidden\":true}]");
        assertThat(entity.getBaseStatsJson())
                .isEqualTo("[{\"name\":\"hp\",\"value\":35},{\"name\":\"speed\",\"value\":90}]");
        assertThat(entity.getTags()).containsExactlyInAnyOrder("starter", "electric");
        assertThat(entity.getLocalizedNames())
                .extracting(LocalizedNameEmbeddable::lang)
                .containsExactlyInAnyOrder("ja", "pt-BR");
    }

    @Test
    void should_persist_new_entities_with_a_null_version() {
        assertThat(mapper.toNewEntity(pikachu(FULL_CUSTOM, 0)).getVersion())
                .as("new entities are persisted, not merged")
                .isNull();
    }

    @Test
    void should_read_absent_geo_back_as_null() {
        CollectedPokemon pokemon = pikachu(CustomAttributes.EMPTY, 0);

        CollectedPokemon back = mapper.toDomain(withVersion(mapper.toNewEntity(pokemon), 3L));

        assertThat(back.custom()).isEqualTo(CustomAttributes.EMPTY);
        assertThat(back.version()).isEqualTo(3);
    }

    @Test
    void should_read_all_blank_geo_back_as_null() {
        CustomAttributes emptyGeo =
                new CustomAttributes(java.util.Set.of(), new GeoMetadata(null, null, null), java.util.Set.of());

        CollectedPokemon back = mapper.toDomain(withVersion(mapper.toNewEntity(pikachu(emptyGeo, 0)), 0L));

        assertThat(back.custom().geo()).isNull();
    }

    @Test
    void should_keep_geo_without_coordinates() {
        CustomAttributes custom = CustomAttributes.of(List.of(), new GeoMetadata("Kanto", null, null), List.of());

        CollectedPokemon back = mapper.toDomain(withVersion(mapper.toNewEntity(pikachu(custom, 0)), 0L));

        assertThat(back.custom().geo()).isEqualTo(new GeoMetadata("Kanto", null, null));
    }

    @Test
    void should_keep_coordinates_exactly() {
        CustomAttributes custom = CustomAttributes.of(
                List.of(), new GeoMetadata(null, null, new Coordinates(35.676191, -139.650310)), List.of());

        CollectedPokemon back = mapper.toDomain(withVersion(mapper.toNewEntity(pikachu(custom, 0)), 0L));

        assertThat(back.custom().geo().coordinates()).isEqualTo(new Coordinates(35.676191, -139.650310));
    }

    @Test
    void should_copy_everything_but_identity_and_version_into_a_managed_entity() {
        CollectedPokemonEntity managed = withVersion(mapper.toNewEntity(pikachu(CustomAttributes.EMPTY, 0)), 4L);
        UUID id = managed.getId();
        CollectedPokemon changed = CollectedPokemon.rehydrate(
                new com.pokedex.domain.collection.vo.CollectedPokemonId(id),
                CollectedPokemonFixtures.PIKACHU,
                FULL_CUSTOM,
                4,
                CollectedPokemonFixtures.SYNCED,
                CollectedPokemonFixtures.UPDATED);

        mapper.copyInto(changed, managed);

        assertThat(managed.getId()).isEqualTo(id);
        assertThat(managed.getVersion()).isEqualTo(4L);
        assertSameState(mapper.toDomain(managed), changed);
    }

    @Test
    void should_fail_loudly_on_corrupt_snapshot_json() {
        CollectedPokemonEntity entity = withVersion(mapper.toNewEntity(pikachu(FULL_CUSTOM, 0)), 0L);
        entity.setAbilitiesJson("{not json");

        assertThatThrownBy(() -> mapper.toDomain(entity)).isInstanceOf(IllegalStateException.class);
    }

    private static CollectedPokemonEntity withVersion(CollectedPokemonEntity entity, Long version) {
        entity.setVersion(version);
        return entity;
    }

    private static void assertSameState(CollectedPokemon actual, CollectedPokemon expected) {
        assertThat(actual.id()).isEqualTo(expected.id());
        assertThat(actual.snapshot()).isEqualTo(expected.snapshot());
        assertThat(actual.custom()).isEqualTo(expected.custom());
        assertThat(actual.version()).isEqualTo(expected.version());
        assertThat(actual.syncedAt()).isEqualTo(expected.syncedAt());
        assertThat(actual.updatedAt()).isEqualTo(expected.updatedAt());
    }
}
