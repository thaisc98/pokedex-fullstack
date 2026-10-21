package com.pokedex.infrastructure.persistence;

import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.BaseStat;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.catalog.vo.StatName;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.collection.CustomAttributes;
import com.pokedex.domain.collection.UpstreamSnapshot;
import com.pokedex.domain.collection.vo.CollectedPokemonId;
import com.pokedex.domain.collection.vo.Coordinates;
import com.pokedex.domain.collection.vo.GeoMetadata;
import com.pokedex.domain.collection.vo.LocalizedName;
import com.pokedex.domain.collection.vo.Tag;
import com.pokedex.infrastructure.persistence.entity.CollectedPokemonEntity;
import com.pokedex.infrastructure.persistence.entity.LocalizedNameEmbeddable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Maps {@link CollectedPokemon} ↔ {@link CollectedPokemonEntity} by hand. The snapshot's abilities and stats
 * are stored as JSON arrays; timestamps are truncated to microseconds, the precision of {@code timestamptz},
 * so what a save returns equals what a later read returns.
 */
public final class CollectedPokemonEntityMapper {

    private static final TypeReference<List<AbilityJson>> ABILITIES = new TypeReference<>() {};
    private static final TypeReference<List<StatJson>> STATS = new TypeReference<>() {};

    private final JsonMapper json = JsonMapper.builder()
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();

    /** A new row with a {@code null} version, so it is persisted rather than merged. */
    public CollectedPokemonEntity toNewEntity(CollectedPokemon pokemon) {
        CollectedPokemonEntity entity = new CollectedPokemonEntity(
                pokemon.id().value(), pokemon.pokedexId().value());
        copyInto(pokemon, entity);
        return entity;
    }

    /** Copies everything except the identity and the version, which belong to the row. */
    public void copyInto(CollectedPokemon pokemon, CollectedPokemonEntity entity) {
        UpstreamSnapshot snapshot = pokemon.snapshot();
        entity.setName(snapshot.name().value());
        entity.setSpriteUrl(snapshot.sprite().value());
        entity.setCategory(snapshot.category().value());
        entity.setWeightHg(snapshot.mass().hectograms());
        entity.setAbilitiesJson(write(snapshot.abilities().stream()
                .map(a -> new AbilityJson(a.name(), a.hidden()))
                .toList()));
        entity.setBaseStatsJson(write(snapshot.stats().stream()
                .map(s -> new StatJson(s.name().slug(), s.value()))
                .toList()));

        CustomAttributes custom = pokemon.custom();
        GeoMetadata geo = custom.geo();
        Coordinates coordinates = geo == null ? null : geo.coordinates();
        entity.setRegion(geo == null ? null : geo.region());
        entity.setHabitat(geo == null ? null : geo.habitat());
        entity.setLatitude(coordinates == null ? null : BigDecimal.valueOf(coordinates.latitude()));
        entity.setLongitude(coordinates == null ? null : BigDecimal.valueOf(coordinates.longitude()));
        replace(entity.getTags(), custom.tags().stream().map(Tag::value).toList());
        replace(
                entity.getLocalizedNames(),
                custom.localizedNames().stream()
                        .map(n -> new LocalizedNameEmbeddable(n.lang(), n.value()))
                        .toList());

        entity.setSyncedAt(micros(pokemon.syncedAt()));
        entity.setUpdatedAt(micros(pokemon.updatedAt()));
    }

    public CollectedPokemon toDomain(CollectedPokemonEntity entity) {
        UpstreamSnapshot snapshot = new UpstreamSnapshot(
                new PokedexId(entity.getPokedexId()),
                new PokemonName(entity.getName()),
                new ImageUrl(entity.getSpriteUrl()),
                new Category(entity.getCategory()),
                new Mass(entity.getWeightHg()),
                read(entity.getAbilitiesJson(), ABILITIES).stream()
                        .map(a -> new Ability(a.name(), a.hidden()))
                        .toList(),
                read(entity.getBaseStatsJson(), STATS).stream()
                        .map(s -> new BaseStat(StatName.fromSlug(s.name()), s.value()))
                        .toList());
        Set<LocalizedName> names = new LinkedHashSet<>();
        entity.getLocalizedNames().forEach(n -> names.add(new LocalizedName(n.lang(), n.value())));
        Set<Tag> tags = new LinkedHashSet<>();
        entity.getTags().forEach(t -> tags.add(new Tag(t)));
        CustomAttributes custom = new CustomAttributes(names, geo(entity), tags);
        return CollectedPokemon.rehydrate(
                new CollectedPokemonId(entity.getId()),
                snapshot,
                custom,
                Objects.requireNonNull(entity.getVersion(), "version"),
                entity.getSyncedAt(),
                entity.getUpdatedAt());
    }

    private static @Nullable GeoMetadata geo(CollectedPokemonEntity entity) {
        BigDecimal latitude = entity.getLatitude();
        BigDecimal longitude = entity.getLongitude();
        Coordinates coordinates = latitude == null || longitude == null
                ? null
                : new Coordinates(latitude.doubleValue(), longitude.doubleValue());
        GeoMetadata geo = new GeoMetadata(entity.getRegion(), entity.getHabitat(), coordinates);
        return geo.isEmpty() ? null : geo;
    }

    private static <T> void replace(Set<T> managed, List<T> values) {
        managed.retainAll(values);
        managed.addAll(values);
    }

    private static Instant micros(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS);
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Could not write the snapshot JSON", e);
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JacksonException e) {
            throw new IllegalStateException("Stored snapshot JSON is corrupt", e);
        }
    }

    private record AbilityJson(String name, boolean hidden) {}

    private record StatJson(String name, int value) {}
}
