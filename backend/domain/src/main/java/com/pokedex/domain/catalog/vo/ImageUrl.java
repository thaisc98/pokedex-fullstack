package com.pokedex.domain.catalog.vo;

import com.pokedex.domain.shared.InvalidValueException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * An image URL that is safe to hand to the browser (security.md §4.5): absolute {@code https}, host
 * {@code raw.githubusercontent.com}, default port, no user info, a path inside {@code /PokeAPI/sprites/}
 * with no dot segments, and at most 2048 characters. The server never fetches it.
 */
public record ImageUrl(String value) {

    public static final int MAX_LENGTH = 2048;

    private static final String ALLOWED_HOST = "raw.githubusercontent.com";
    private static final String ALLOWED_PATH_PREFIX = "/PokeAPI/sprites/";
    private static final int HTTPS_PORT = 443;
    private static final int DEFAULT_PORT = -1;

    /** Shown when the upstream image is missing or not allowed. */
    public static final ImageUrl PLACEHOLDER =
            new ImageUrl("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/poke-ball.png");

    public ImageUrl {
        Objects.requireNonNull(value, "value");
        if (value.length() > MAX_LENGTH || !isAllowed(value)) {
            throw new InvalidValueException(
                    "imageUrl", "must be an https URL of at most 2048 characters on the allowed image host");
        }
    }

    /** The URL if it passes the rules, otherwise {@link #PLACEHOLDER}. */
    public static ImageUrl orPlaceholder(@Nullable String value) {
        if (value == null) {
            return PLACEHOLDER;
        }
        try {
            return new ImageUrl(value);
        } catch (InvalidValueException e) {
            return PLACEHOLDER;
        }
    }

    private static boolean isAllowed(String value) {
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            return false;
        }
        return "https".equals(uri.getScheme())
                && !uri.isOpaque()
                && uri.getRawUserInfo() == null
                && ALLOWED_HOST.equalsIgnoreCase(uri.getHost())
                && (uri.getPort() == DEFAULT_PORT || uri.getPort() == HTTPS_PORT)
                && hasAllowedPath(uri);
    }

    private static boolean hasAllowedPath(URI uri) {
        String rawPath = uri.getRawPath();
        String decodedPath = uri.getPath();
        return rawPath != null
                && rawPath.startsWith(ALLOWED_PATH_PREFIX)
                && Arrays.stream(decodedPath.split("/", -1))
                        .noneMatch(segment -> segment.equals(".") || segment.equals(".."));
    }
}
