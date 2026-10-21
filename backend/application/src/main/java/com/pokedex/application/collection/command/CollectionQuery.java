package com.pokedex.application.collection.command;

import com.pokedex.domain.collection.vo.Tag;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.domain.shared.PageRequest;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * UC-05 list filters. {@code tag} is an exact match; {@code name} is a stripped, lowercased literal
 * substring of the snapshot name (1-50 characters). LIKE wildcards stay literal: escaping them is the
 * persistence adapter's job (security.md §4.4).
 */
public record CollectionQuery(
        PageRequest page, @Nullable Tag tag, @Nullable String name) {

    public static final int MAX_NAME_LENGTH = 50;

    public CollectionQuery {
        Objects.requireNonNull(page, "page");
        if (name != null) {
            name = name.strip().toLowerCase(Locale.ROOT);
            if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
                throw new InvalidValueException("q", "must be 1-50 characters");
            }
        }
    }

    public static CollectionQuery firstPage() {
        return new CollectionQuery(PageRequest.ofPage(0), null, null);
    }

    /** From raw request parameters; {@code null} means "no filter", a blank {@code q} is rejected. */
    public static CollectionQuery of(PageRequest page, @Nullable String rawTag, @Nullable String rawQ) {
        return new CollectionQuery(page, rawTag == null ? null : new Tag(rawTag), rawQ);
    }
}
