package com.pokedex.infrastructure.pokeapi;

import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.infrastructure.pokeapi.dto.NamedResourceDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Hand-written PokeAPI DTO → domain mapper. Upstream data is untrusted: every value passes the domain value
 * objects, and upstream URLs are only mined for a numeric id with a strict pattern (security.md §4.5). The
 * host in those URLs is ignored; callers rebuild the request against the configured base.
 */
public final class PokeApiMapper {

    private static final String ENGLISH = "en";

    private static final Pattern POKEMON_URL = resourceUrl("pokemon");
    private static final Pattern SPECIES_URL = resourceUrl("pokemon-species");

    private static Pattern resourceUrl(String resource) {
        return Pattern.compile(
                "https?://[A-Za-z0-9.-]+(?::[0-9]{1,5})?(?:/[A-Za-z0-9._~-]+)*/" + resource + "/([1-9][0-9]{0,6})/?");
    }

    public PokemonSummary toSummary(PokeApiPokemonDto pokemon, PokeApiSpeciesDto species) {
        PokeApiPokemonDto.Sprites sprites = pokemon.sprites();
        return new PokemonSummary(
                new PokedexId(Objects.requireNonNull(pokemon.id(), "id")),
                new PokemonName(Objects.requireNonNull(pokemon.name(), "name")),
                ImageUrl.orPlaceholder(sprites == null ? null : sprites.frontDefault()),
                category(species),
                new Mass(Objects.requireNonNull(pokemon.weight(), "weight")),
                abilities(pokemon));
    }

    /** Pokémon id from a list result URL such as {@code https://pokeapi.co/api/v2/pokemon/25/}. */
    public int pokemonId(@Nullable String url) {
        return extractId(POKEMON_URL, url);
    }

    /** Species id from {@code species.url}; it differs from the Pokémon id for forms (10264 → 1007). */
    public int speciesId(PokeApiPokemonDto pokemon) {
        NamedResourceDto species = Objects.requireNonNull(pokemon.species(), "species");
        return extractId(SPECIES_URL, species.url());
    }

    private static int extractId(Pattern pattern, @Nullable String url) {
        Matcher matcher = pattern.matcher(url == null ? "" : url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Upstream resource URL does not match the expected pattern");
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static Category category(PokeApiSpeciesDto species) {
        List<PokeApiSpeciesDto.Genus> genera = species.genera();
        if (genera == null) {
            return Category.UNKNOWN;
        }
        return genera.stream()
                .filter(g -> g.language() != null && ENGLISH.equals(g.language().name()))
                .map(PokeApiSpeciesDto.Genus::genus)
                .filter(Objects::nonNull)
                .findFirst()
                .map(Category::orUnknown)
                .orElse(Category.UNKNOWN);
    }

    private static List<Ability> abilities(PokeApiPokemonDto pokemon) {
        List<PokeApiPokemonDto.AbilitySlot> slots = pokemon.abilities();
        if (slots == null) {
            return List.of();
        }
        return slots.stream()
                .map(slot -> {
                    NamedResourceDto ability = Objects.requireNonNull(slot.ability(), "ability");
                    return new Ability(Objects.requireNonNull(ability.name(), "ability.name"), slot.hidden());
                })
                .toList();
    }
}
