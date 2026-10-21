package com.pokedex.domain.catalog.vo;

import com.pokedex.domain.shared.NotFoundException;
import java.util.Locale;
import java.util.Objects;

/**
 * How a caller names a Pokémon in {@code /pokemon/{idOrName}} (architecture.md §2.8): by Pokédex id or by name.
 */
public sealed interface PokemonRef permits PokemonRef.ById, PokemonRef.ByName {

    /** Digits after the leading zeros beyond this can never fit an {@code int}. */
    int MAX_ID_DIGITS = 10;

    record ById(PokedexId id) implements PokemonRef {

        public ById {
            Objects.requireNonNull(id, "id");
        }
    }

    record ByName(PokemonName name) implements PokemonRef {

        public ByName {
            Objects.requireNonNull(name, "name");
        }
    }

    /**
     * All ASCII digits → an id; {@code 0} or anything beyond {@code int} cannot exist, so {@link
     * NotFoundException}. Anything else is lowercased and must be a valid {@link PokemonName}, otherwise {@link
     * com.pokedex.domain.shared.InvalidValueException}.
     */
    static PokemonRef parse(String raw) {
        Objects.requireNonNull(raw, "raw");
        if (!raw.isEmpty() && raw.chars().allMatch(c -> c >= '0' && c <= '9')) {
            String digits = raw.replaceFirst("^0+", "");
            if (digits.isEmpty() || digits.length() > MAX_ID_DIGITS || Long.parseLong(digits) > Integer.MAX_VALUE) {
                throw new NotFoundException("pokemon");
            }
            return new ById(new PokedexId(Integer.parseInt(digits)));
        }
        return new ByName(new PokemonName(raw.toLowerCase(Locale.ROOT)));
    }
}
