package com.pokedex.domain.collection.vo;

import com.pokedex.domain.shared.InvalidValueException;
import org.jspecify.annotations.Nullable;

/** Optional geographic metadata: region and habitat (≤ 60 characters, blank means absent) and coordinates. */
public record GeoMetadata(
        @Nullable String region,
        @Nullable String habitat,
        @Nullable Coordinates coordinates) {

    public static final int MAX_LENGTH = 60;

    public GeoMetadata {
        region = optionalText(region, "geo.region");
        habitat = optionalText(habitat, "geo.habitat");
    }

    public boolean isEmpty() {
        return region == null && habitat == null && coordinates == null;
    }

    private static @Nullable String optionalText(@Nullable String raw, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.strip();
        if (text.codePointCount(0, text.length()) > MAX_LENGTH) {
            throw new InvalidValueException(field, "must be at most 60 characters");
        }
        return text;
    }
}
