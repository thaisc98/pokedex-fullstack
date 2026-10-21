package com.pokedex.domain.collection.vo;

import com.pokedex.domain.shared.InvalidValueException;
import org.jspecify.annotations.Nullable;

/**
 * Optional geographic metadata. Region and habitat are each absent ({@code null}) or 1-60 characters after
 * stripping; a present but blank value is rejected. Coordinates are optional.
 */
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
        if (raw == null) {
            return null;
        }
        String text = raw.strip();
        int length = text.codePointCount(0, text.length());
        if (length < 1 || length > MAX_LENGTH) {
            throw new InvalidValueException(field, "must be 1-60 characters");
        }
        return text;
    }
}
