package com.pokedex.domain.collection;

import com.pokedex.domain.collection.vo.GeoMetadata;
import com.pokedex.domain.collection.vo.LocalizedName;
import com.pokedex.domain.collection.vo.Tag;
import com.pokedex.domain.shared.InvalidValueException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * The only editable part of a collected Pokémon (BR-06): localized names (at most one per language), geo
 * metadata and up to 10 normalized, de-duplicated tags (BR-07). Iteration order follows the input.
 */
public record CustomAttributes(
        Set<LocalizedName> localizedNames, @Nullable GeoMetadata geo, Set<Tag> tags) {

    public static final int MAX_TAGS = 10;

    public static final CustomAttributes EMPTY = new CustomAttributes(Set.of(), null, Set.of());

    public CustomAttributes {
        localizedNames = readOnlyCopy(Objects.requireNonNull(localizedNames, "localizedNames"));
        tags = readOnlyCopy(Objects.requireNonNull(tags, "tags"));
        if (tags.size() > MAX_TAGS) {
            throw new InvalidValueException("tags", "must contain at most 10 tags");
        }
        Set<String> languages = new HashSet<>();
        for (LocalizedName name : localizedNames) {
            if (!languages.add(name.lang())) {
                throw new InvalidValueException("localizedNames", "must contain at most one name per language");
            }
        }
    }

    /** Builds from raw tag strings: each becomes a {@link Tag}, duplicates collapse, then the cap applies. */
    public static CustomAttributes of(
            List<LocalizedName> localizedNames, @Nullable GeoMetadata geo, List<String> rawTags) {
        Objects.requireNonNull(rawTags, "rawTags");
        Set<Tag> tags = new LinkedHashSet<>();
        for (String raw : rawTags) {
            tags.add(new Tag(raw));
        }
        return new CustomAttributes(
                new LinkedHashSet<>(Objects.requireNonNull(localizedNames, "localizedNames")), geo, tags);
    }

    private static <T> Set<T> readOnlyCopy(Collection<T> source) {
        Set<T> copy = new LinkedHashSet<>();
        for (T item : source) {
            copy.add(Objects.requireNonNull(item, "element"));
        }
        return Collections.unmodifiableSet(copy);
    }
}
