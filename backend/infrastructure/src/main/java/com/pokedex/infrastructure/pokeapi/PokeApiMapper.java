package com.pokedex.infrastructure.pokeapi;

import com.pokedex.domain.catalog.EvolutionChain;
import com.pokedex.domain.catalog.EvolutionNode;
import com.pokedex.domain.catalog.PokemonDetail;
import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.BaseStat;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.Description;
import com.pokedex.domain.catalog.vo.EvolutionTrigger;
import com.pokedex.domain.catalog.vo.Height;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.catalog.vo.PokemonType;
import com.pokedex.domain.catalog.vo.StatName;
import com.pokedex.infrastructure.pokeapi.dto.NamedResourceDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiEvolutionChainDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
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
    private static final Pattern EVOLUTION_CHAIN_URL = resourceUrl("evolution-chain");
    private static final Pattern VERSION_URL = resourceUrl("version");

    /** Trigger name used when PokeAPI lists no evolution detail for a later stage. */
    static final String UNKNOWN_TRIGGER = "unknown";

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

    /** Evolution chain id from {@code species.evolution_chain.url}. */
    public int evolutionChainId(PokeApiSpeciesDto species) {
        NamedResourceDto chain = Objects.requireNonNull(species.evolutionChain(), "evolution_chain");
        return extractId(EVOLUTION_CHAIN_URL, chain.url());
    }

    /**
     * US02 detail. Artwork: official artwork, else the sprite, else the placeholder (each must pass {@link
     * ImageUrl}). Description: the English flavor text of the latest game version that has one (BR-03).
     */
    public PokemonDetail toDetail(
            PokeApiPokemonDto pokemon, PokeApiSpeciesDto species, PokeApiEvolutionChainDto evolutionChain) {
        return new PokemonDetail(
                new PokedexId(Objects.requireNonNull(pokemon.id(), "id")),
                new PokemonName(Objects.requireNonNull(pokemon.name(), "name")),
                artwork(pokemon),
                stats(pokemon),
                description(species),
                new EvolutionChain(node(Objects.requireNonNull(evolutionChain.chain(), "chain"), true)),
                types(pokemon),
                new Height(Objects.requireNonNull(pokemon.height(), "height")),
                new Mass(Objects.requireNonNull(pokemon.weight(), "weight")));
    }

    private static ImageUrl artwork(PokeApiPokemonDto pokemon) {
        PokeApiPokemonDto.Sprites sprites = pokemon.sprites();
        if (sprites == null) {
            return ImageUrl.PLACEHOLDER;
        }
        PokeApiPokemonDto.OtherSprites other = sprites.other();
        PokeApiPokemonDto.Artwork official = other == null ? null : other.officialArtwork();
        ImageUrl artwork = ImageUrl.orPlaceholder(official == null ? null : official.frontDefault());
        return artwork != ImageUrl.PLACEHOLDER ? artwork : ImageUrl.orPlaceholder(sprites.frontDefault());
    }

    private static List<BaseStat> stats(PokeApiPokemonDto pokemon) {
        return Objects.requireNonNull(pokemon.stats(), "stats").stream()
                .map(slot -> new BaseStat(
                        StatName.fromSlug(Objects.requireNonNull(
                                Objects.requireNonNull(slot.stat(), "stat").name(), "stat.name")),
                        Objects.requireNonNull(slot.baseStat(), "base_stat")))
                .toList();
    }

    private static List<PokemonType> types(PokeApiPokemonDto pokemon) {
        return Objects.requireNonNull(pokemon.types(), "types").stream()
                .map(slot -> PokemonType.fromSlug(Objects.requireNonNull(
                        Objects.requireNonNull(slot.type(), "type").name(), "type.name")))
                .toList();
    }

    /** Highest version id (taken from {@code version.url} with a strict pattern) among English entries. */
    private static Description description(PokeApiSpeciesDto species) {
        List<PokeApiSpeciesDto.FlavorText> entries = species.flavorTextEntries();
        if (entries == null) {
            return Description.EMPTY;
        }
        String latest = null;
        int latestVersion = 0;
        for (PokeApiSpeciesDto.FlavorText entry : entries) {
            OptionalInt version = englishVersion(entry);
            if (version.isPresent() && entry.text() != null && version.getAsInt() >= latestVersion) {
                latestVersion = version.getAsInt();
                latest = entry.text();
            }
        }
        return latest == null ? Description.EMPTY : new Description(latest);
    }

    private static OptionalInt englishVersion(PokeApiSpeciesDto.FlavorText entry) {
        NamedResourceDto language = entry.language();
        NamedResourceDto version = entry.version();
        if (language == null || !ENGLISH.equals(language.name()) || version == null || version.url() == null) {
            return OptionalInt.empty();
        }
        Matcher matcher = VERSION_URL.matcher(version.url());
        return matcher.matches() ? OptionalInt.of(Integer.parseInt(matcher.group(1))) : OptionalInt.empty();
    }

    private EvolutionNode node(PokeApiEvolutionChainDto.ChainLink link, boolean root) {
        NamedResourceDto species = Objects.requireNonNull(link.species(), "species");
        List<PokeApiEvolutionChainDto.ChainLink> next = link.evolvesTo() == null ? List.of() : link.evolvesTo();
        return new EvolutionNode(
                new PokedexId(extractId(SPECIES_URL, species.url())),
                new PokemonName(Objects.requireNonNull(species.name(), "species.name")),
                root ? null : trigger(link.evolutionDetails()),
                next.stream().map(child -> node(child, false)).toList());
    }

    /** Several details: the {@code is_default} one wins, else the first (lead decision). None: unknown. */
    private static EvolutionTrigger trigger(@Nullable List<PokeApiEvolutionChainDto.EvolutionDetail> details) {
        if (details == null || details.isEmpty()) {
            return new EvolutionTrigger.Other(UNKNOWN_TRIGGER);
        }
        PokeApiEvolutionChainDto.EvolutionDetail chosen = details.stream()
                .filter(detail -> Boolean.TRUE.equals(detail.isDefault()))
                .findFirst()
                .orElse(details.getFirst());
        String name = Objects.requireNonNull(
                Objects.requireNonNull(chosen.trigger(), "trigger").name(), "trigger.name");
        return switch (name) {
            case "level-up" ->
                chosen.minLevel() == null
                        ? EvolutionTrigger.LevelUp.anyLevel()
                        : EvolutionTrigger.LevelUp.atLevel(chosen.minLevel());
            case "use-item" ->
                new EvolutionTrigger.UseItem(Objects.requireNonNull(
                        Objects.requireNonNull(chosen.item(), "item").name(), "item.name"));
            case "trade" -> new EvolutionTrigger.Trade();
            default -> new EvolutionTrigger.Other(name);
        };
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
