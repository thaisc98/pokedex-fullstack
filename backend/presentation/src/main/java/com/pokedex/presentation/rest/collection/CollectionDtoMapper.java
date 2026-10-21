package com.pokedex.presentation.rest.collection;

import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.collection.CustomAttributes;
import com.pokedex.domain.collection.UpstreamSnapshot;
import com.pokedex.domain.collection.vo.Coordinates;
import com.pokedex.domain.collection.vo.GeoMetadata;
import com.pokedex.domain.collection.vo.LocalizedName;
import com.pokedex.domain.collection.vo.Tag;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.domain.shared.Page;
import com.pokedex.presentation.rest.catalog.dto.AbilityDto;
import com.pokedex.presentation.rest.catalog.dto.BaseStatDto;
import com.pokedex.presentation.rest.collection.dto.CollectedPokemonDto;
import com.pokedex.presentation.rest.collection.dto.CoordinatesDto;
import com.pokedex.presentation.rest.collection.dto.CustomAttributesDto;
import com.pokedex.presentation.rest.collection.dto.GeoMetadataDto;
import com.pokedex.presentation.rest.collection.dto.LocalizedNameDto;
import com.pokedex.presentation.rest.collection.dto.UpstreamSnapshotDto;
import com.pokedex.presentation.rest.shared.PageResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/**
 * Collection REST DTOs ↔ domain, by hand. Requests are turned into domain value objects here; a broken rule is
 * re-reported on the request's Spring {@code FieldError}-style path (e.g. {@code customAttributes.tags[2]}) with the
 * domain's message, which never contains the rejected value.
 */
final class CollectionDtoMapper {

    static final String CUSTOM = "customAttributes";
    private static final String REQUIRED = "must not be null";

    private CollectionDtoMapper() {}

    // ------------------------------------------------------------ responses

    static CollectedPokemonDto toDto(CollectedPokemon pokemon) {
        return new CollectedPokemonDto(
                pokemon.pokedexId().value(),
                toDto(pokemon.snapshot()),
                toDto(pokemon.custom()),
                pokemon.version(),
                pokemon.syncedAt(),
                pokemon.updatedAt());
    }

    static PageResponse<CollectedPokemonDto> toResponse(Page<CollectedPokemon> page) {
        return new PageResponse<>(
                page.content().stream().map(CollectionDtoMapper::toDto).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }

    private static UpstreamSnapshotDto toDto(UpstreamSnapshot snapshot) {
        return new UpstreamSnapshotDto(
                snapshot.id().value(),
                snapshot.name().value(),
                snapshot.sprite().value(),
                snapshot.category().value(),
                snapshot.mass().kilograms(),
                snapshot.abilities().stream()
                        .map(a -> new AbilityDto(a.name(), a.hidden()))
                        .toList(),
                snapshot.stats().stream()
                        .map(s -> new BaseStatDto(s.name().slug(), s.value()))
                        .toList());
    }

    private static CustomAttributesDto toDto(CustomAttributes custom) {
        GeoMetadata geo = custom.geo();
        return new CustomAttributesDto(
                custom.localizedNames().stream()
                        .map(n -> new LocalizedNameDto(n.lang(), n.value()))
                        .toList(),
                geo == null ? null : toDto(geo),
                custom.tags().stream().map(Tag::value).toList());
    }

    private static GeoMetadataDto toDto(GeoMetadata geo) {
        Coordinates c = geo.coordinates();
        return new GeoMetadataDto(
                geo.region(), geo.habitat(), c == null ? null : new CoordinatesDto(c.latitude(), c.longitude()));
    }

    // ------------------------------------------------------------ requests

    static CustomAttributes toCustomAttributes(CustomAttributesDto request) {
        List<@Nullable LocalizedNameDto> rawNames = required(request.localizedNames(), CUSTOM + ".localizedNames");
        List<@Nullable String> rawTags = required(request.tags(), CUSTOM + ".tags");

        List<LocalizedName> names = new ArrayList<>(rawNames.size());
        for (int i = 0; i < rawNames.size(); i++) {
            String path = CUSTOM + ".localizedNames[" + i + "]";
            LocalizedNameDto raw = required(rawNames.get(i), path);
            String lang = required(raw.lang(), path + ".lang");
            String value = required(raw.value(), path + ".value");
            names.add(renamed(() -> new LocalizedName(lang, value), f -> path + f.substring("localizedName".length())));
        }
        List<String> tags = new ArrayList<>(rawTags.size());
        for (int i = 0; i < rawTags.size(); i++) {
            String path = CUSTOM + ".tags[" + i + "]";
            String tag = required(rawTags.get(i), path);
            tags.add(renamed(() -> new Tag(tag), f -> path).value());
        }
        GeoMetadataDto rawGeo = request.geo();
        GeoMetadata geo = rawGeo == null ? null : toGeo(rawGeo);
        return renamed(() -> CustomAttributes.of(names, geo, tags), f -> CUSTOM + "." + f);
    }

    private static GeoMetadata toGeo(GeoMetadataDto raw) {
        CoordinatesDto rawCoordinates = raw.coordinates();
        Coordinates coordinates = null;
        if (rawCoordinates != null) {
            String path = CUSTOM + ".geo.coordinates";
            double latitude = required(rawCoordinates.latitude(), path + ".latitude");
            double longitude = required(rawCoordinates.longitude(), path + ".longitude");
            coordinates = renamed(() -> new Coordinates(latitude, longitude), f -> CUSTOM + ".geo." + f);
        }
        Coordinates resolved = coordinates;
        return renamed(() -> new GeoMetadata(raw.region(), raw.habitat(), resolved), f -> CUSTOM + "." + f);
    }

    /** Runs {@code factory}; a violation is re-reported on {@code rename(domainField)}, same message. */
    private static <T> T renamed(Supplier<T> factory, UnaryOperator<String> rename) {
        try {
            return factory.get();
        } catch (InvalidValueException e) {
            throw new InvalidValueException(rename.apply(e.field()), Objects.requireNonNull(e.getMessage()));
        }
    }

    private static <T> T required(@Nullable T value, String field) {
        if (value == null) {
            throw new InvalidValueException(field, REQUIRED);
        }
        return value;
    }
}
