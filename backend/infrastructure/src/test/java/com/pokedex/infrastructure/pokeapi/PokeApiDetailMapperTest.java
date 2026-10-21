package com.pokedex.infrastructure.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.catalog.EvolutionNode;
import com.pokedex.domain.catalog.PokemonDetail;
import com.pokedex.domain.catalog.vo.BaseStat;
import com.pokedex.domain.catalog.vo.EvolutionTrigger;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonType;
import com.pokedex.domain.catalog.vo.StatName;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiEvolutionChainDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** PokeAPI pokemon + species + evolution-chain → {@code PokemonDetail}, driven by the recorded fixtures. */
class PokeApiDetailMapperTest {

    private static final String SPRITES = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/";

    private final PokeApiMapper mapper = new PokeApiMapper();

    @Test
    void should_map_pikachu_to_a_full_detail() {
        PokemonDetail detail = pikachu(PokeApiFixtures.pokemon(25), PokeApiFixtures.species(25));

        assertThat(detail.id().value()).isEqualTo(25);
        assertThat(detail.name().value()).isEqualTo("pikachu");
        assertThat(detail.artwork().value()).isEqualTo(SPRITES + "other/official-artwork/25.png");
        assertThat(detail.types()).containsExactly(PokemonType.ELECTRIC);
        assertThat(detail.stats()).extracting(BaseStat::name).containsExactly(StatName.values());
        assertThat(detail.stats()).extracting(BaseStat::value).containsExactly(35, 55, 40, 50, 50, 90);
        assertThat(detail.height().meters()).isEqualTo(0.4);
        assertThat(detail.mass().kilograms()).isEqualTo(6.0);
    }

    @Test
    void should_use_the_english_flavor_text_of_the_latest_version_that_has_one() {
        // legends-arceus (version 39) is the newest English entry in the recorded species
        PokemonDetail detail = pikachu(PokeApiFixtures.pokemon(25), PokeApiFixtures.species(25));

        assertThat(detail.description().value())
                .isEqualTo("Possesses cheek sacs in which it stores electricity. This clever forest-dweller roasts"
                        + " tough berries with an electric shock before consuming them.");
    }

    @Test
    void should_clean_the_chosen_flavor_text() {
        PokemonDetail detail = pikachu(PokeApiFixtures.pokemon(25), speciesWithEnglishVersions(List.of("red")));

        assertThat(detail.description().value())
                .isEqualTo("When several of these POKéMON gather, their electricity could build and cause"
                        + " lightning storms.");
    }

    @Test
    void should_return_an_empty_description_without_any_english_flavor_text() {
        PokemonDetail detail = pikachu(PokeApiFixtures.pokemon(25), speciesWithEnglishVersions(List.of()));

        assertThat(detail.description().isEmpty()).isTrue();
    }

    @Test
    void should_return_an_empty_description_when_flavor_texts_are_missing() {
        ObjectNode species = PokeApiFixtures.tree("pokemon-species/25.json");
        species.remove("flavor_text_entries");

        PokemonDetail detail =
                pikachu(PokeApiFixtures.pokemon(25), PokeApiFixtures.convert(species, PokeApiSpeciesDto.class));

        assertThat(detail.description().isEmpty()).isTrue();
    }

    @Test
    void should_ignore_a_flavor_text_whose_version_url_does_not_match_the_strict_pattern() {
        ObjectNode species = PokeApiFixtures.tree("pokemon-species/25.json");
        for (JsonNode entry : species.get("flavor_text_entries")) {
            if ("legends-arceus".equals(entry.get("version").get("name").asString())) {
                ((ObjectNode) entry.get("version")).put("url", "https://pokeapi.co/api/v2/version/latest/");
            }
        }

        PokemonDetail detail =
                pikachu(PokeApiFixtures.pokemon(25), PokeApiFixtures.convert(species, PokeApiSpeciesDto.class));

        assertThat(detail.description().value()).doesNotStartWith("Possesses cheek sacs");
    }

    @Test
    void should_fall_back_to_the_sprite_when_official_artwork_is_missing() {
        ObjectNode pokemon = PokeApiFixtures.tree("pokemon/25.json");
        ((ObjectNode) pokemon.get("sprites").get("other").get("official-artwork")).putNull("front_default");

        PokemonDetail detail =
                pikachu(PokeApiFixtures.convert(pokemon, PokeApiPokemonDto.class), PokeApiFixtures.species(25));

        assertThat(detail.artwork().value()).isEqualTo(SPRITES + "25.png");
    }

    @Test
    void should_fall_back_to_the_sprite_when_official_artwork_is_not_allowed() {
        ObjectNode pokemon = PokeApiFixtures.tree("pokemon/25.json");
        ((ObjectNode) pokemon.get("sprites").get("other").get("official-artwork"))
                .put("front_default", "https://evil.example/25.png");

        PokemonDetail detail =
                pikachu(PokeApiFixtures.convert(pokemon, PokeApiPokemonDto.class), PokeApiFixtures.species(25));

        assertThat(detail.artwork().value()).isEqualTo(SPRITES + "25.png");
    }

    @Test
    void should_use_the_placeholder_when_artwork_and_sprite_are_both_missing() {
        PokemonDetail detail = mapper.toDetail(
                PokeApiFixtures.pokemon(10264), PokeApiFixtures.species(1007), PokeApiFixtures.evolutionChain(535));

        assertThat(detail.artwork()).isEqualTo(ImageUrl.PLACEHOLDER);
        assertThat(detail.types()).containsExactly(PokemonType.FIGHTING, PokemonType.DRAGON);
        assertThat(detail.evolution().root().name().value()).isEqualTo("koraidon");
        assertThat(detail.evolution().root().evolvesTo()).isEmpty();
    }

    @Test
    void should_use_the_placeholder_when_the_sprites_block_is_missing() {
        ObjectNode pokemon = PokeApiFixtures.tree("pokemon/25.json");
        pokemon.remove("sprites");

        PokemonDetail detail =
                pikachu(PokeApiFixtures.convert(pokemon, PokeApiPokemonDto.class), PokeApiFixtures.species(25));

        assertThat(detail.artwork()).isEqualTo(ImageUrl.PLACEHOLDER);
    }

    // ------------------------------------------------------------ evolution (BR-04)

    @Test
    void should_map_a_baby_three_stage_chain_with_triggers() {
        PokemonDetail detail = pikachu(PokeApiFixtures.pokemon(25), PokeApiFixtures.species(25));

        EvolutionNode pichu = detail.evolution().root();
        assertThat(pichu.speciesId()).isEqualTo(new PokedexId(172));
        assertThat(pichu.trigger()).isNull();
        EvolutionNode pikachu = pichu.evolvesTo().getFirst();
        assertThat(pikachu.name().value()).isEqualTo("pikachu");
        assertThat(pikachu.trigger()).isEqualTo(EvolutionTrigger.LevelUp.anyLevel());
        assertThat(pikachu.evolvesTo().getFirst().trigger()).isEqualTo(new EvolutionTrigger.UseItem("thunder-stone"));
    }

    @Test
    void should_keep_all_eight_eevee_branches_and_prefer_the_default_evolution_detail() {
        PokemonDetail detail = mapper.toDetail(
                PokeApiFixtures.pokemon(133), PokeApiFixtures.species(133), PokeApiFixtures.evolutionChain(67));

        List<EvolutionNode> branches = detail.evolution().root().evolvesTo();
        assertThat(branches)
                .extracting(n -> n.name().value())
                .containsExactly(
                        "vaporeon", "jolteon", "flareon", "espeon", "umbreon", "leafeon", "glaceon", "sylveon");
        assertThat(branches)
                .extracting(EvolutionNode::trigger)
                .containsExactly(
                        new EvolutionTrigger.UseItem("water-stone"),
                        new EvolutionTrigger.UseItem("thunder-stone"),
                        new EvolutionTrigger.UseItem("fire-stone"),
                        EvolutionTrigger.LevelUp.anyLevel(),
                        EvolutionTrigger.LevelUp.anyLevel(),
                        // several details: the is_default one wins (lead decision)
                        new EvolutionTrigger.UseItem("leaf-stone"),
                        new EvolutionTrigger.UseItem("ice-stone"),
                        EvolutionTrigger.LevelUp.anyLevel());
    }

    @Test
    void should_use_the_first_evolution_detail_when_none_is_the_default() {
        ObjectNode chain = PokeApiFixtures.tree("evolution-chain/67.json");
        JsonNode leafeon = chain.get("chain").get("evolves_to").get(5);
        for (JsonNode detail : leafeon.get("evolution_details")) {
            ((ObjectNode) detail).put("is_default", false);
        }

        PokemonDetail detail = mapper.toDetail(
                PokeApiFixtures.pokemon(133),
                PokeApiFixtures.species(133),
                PokeApiFixtures.convert(chain, PokeApiEvolutionChainDto.class));

        assertThat(detail.evolution().root().evolvesTo().get(5).trigger())
                .isEqualTo(EvolutionTrigger.LevelUp.anyLevel());
    }

    @Test
    void should_keep_the_minimum_level_of_a_level_up() {
        PokemonDetail detail = mapper.toDetail(
                PokeApiFixtures.pokemon(122), PokeApiFixtures.species(122), PokeApiFixtures.evolutionChain(57));

        EvolutionNode mimeJr = detail.evolution().root();
        assertThat(mimeJr.name().value()).isEqualTo("mime-jr");
        EvolutionNode mrMime = mimeJr.evolvesTo().getFirst();
        assertThat(mrMime.evolvesTo().getFirst().name().value()).isEqualTo("mr-rime");
        assertThat(mrMime.evolvesTo().getFirst().trigger()).isEqualTo(EvolutionTrigger.LevelUp.atLevel(42));
        assertThat(detail.types()).containsExactly(PokemonType.PSYCHIC, PokemonType.FAIRY);
    }

    @Test
    void should_map_trade_and_unknown_triggers() {
        ObjectNode chain = PokeApiFixtures.tree("evolution-chain/10.json");
        ObjectNode pikachu = (ObjectNode) chain.get("chain").get("evolves_to").get(0);
        ArrayNode details = (ArrayNode) pikachu.get("evolution_details");
        details.removeAll();
        details.addObject().putObject("trigger").put("name", "trade").put("url", "x");
        ObjectNode raichu = (ObjectNode) pikachu.get("evolves_to").get(0);
        ArrayNode raichuDetails = (ArrayNode) raichu.get("evolution_details");
        raichuDetails.removeAll();
        raichuDetails
                .addObject()
                .putObject("trigger")
                .put("name", "tower-of-darkness")
                .put("url", "x");

        PokemonDetail detail = pikachu(
                PokeApiFixtures.pokemon(25),
                PokeApiFixtures.species(25),
                PokeApiFixtures.convert(chain, PokeApiEvolutionChainDto.class));

        EvolutionNode pikachuNode = detail.evolution().root().evolvesTo().getFirst();
        assertThat(pikachuNode.trigger()).isEqualTo(new EvolutionTrigger.Trade());
        assertThat(pikachuNode.evolvesTo().getFirst().trigger())
                .isEqualTo(new EvolutionTrigger.Other("tower-of-darkness"));
    }

    @Test
    void should_map_a_later_stage_without_any_evolution_detail_to_an_unknown_trigger() {
        ObjectNode chain = PokeApiFixtures.tree("evolution-chain/10.json");
        ((ArrayNode) chain.get("chain").get("evolves_to").get(0).get("evolution_details")).removeAll();

        PokemonDetail detail = pikachu(
                PokeApiFixtures.pokemon(25),
                PokeApiFixtures.species(25),
                PokeApiFixtures.convert(chain, PokeApiEvolutionChainDto.class));

        assertThat(detail.evolution().root().evolvesTo().getFirst().trigger())
                .isEqualTo(new EvolutionTrigger.Other("unknown"));
    }

    @Test
    void should_extract_the_evolution_chain_id_from_the_species() {
        assertThat(mapper.evolutionChainId(PokeApiFixtures.species(25))).isEqualTo(10);
        assertThat(mapper.evolutionChainId(PokeApiFixtures.species(133))).isEqualTo(67);
    }

    @Test
    void should_reject_a_species_whose_evolution_chain_url_is_missing_or_not_strict() {
        ObjectNode species = PokeApiFixtures.tree("pokemon-species/25.json");
        ((ObjectNode) species.get("evolution_chain")).put("url", "https://pokeapi.co/api/v2/evolution-chain/../x");
        PokeApiSpeciesDto bad = PokeApiFixtures.convert(species, PokeApiSpeciesDto.class);
        species.remove("evolution_chain");
        PokeApiSpeciesDto missing = PokeApiFixtures.convert(species, PokeApiSpeciesDto.class);

        assertThatThrownBy(() -> mapper.evolutionChainId(bad)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.evolutionChainId(missing)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_reject_an_unknown_type_or_stat() {
        ObjectNode unknownType = PokeApiFixtures.tree("pokemon/25.json");
        ((ObjectNode) unknownType.get("types").get(0).get("type")).put("name", "stellar");
        ObjectNode unknownStat = PokeApiFixtures.tree("pokemon/25.json");
        ((ObjectNode) unknownStat.get("stats").get(0).get("stat")).put("name", "accuracy");

        assertThatThrownBy(() -> pikachu(
                        PokeApiFixtures.convert(unknownType, PokeApiPokemonDto.class), PokeApiFixtures.species(25)))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> pikachu(
                        PokeApiFixtures.convert(unknownStat, PokeApiPokemonDto.class), PokeApiFixtures.species(25)))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void should_reject_a_missing_height() {
        ObjectNode pokemon = PokeApiFixtures.tree("pokemon/25.json");
        pokemon.remove("height");

        assertThatThrownBy(() ->
                        pikachu(PokeApiFixtures.convert(pokemon, PokeApiPokemonDto.class), PokeApiFixtures.species(25)))
                .isInstanceOf(NullPointerException.class);
    }

    private PokemonDetail pikachu(PokeApiPokemonDto pokemon, PokeApiSpeciesDto species) {
        return pikachu(pokemon, species, PokeApiFixtures.evolutionChain(10));
    }

    private PokemonDetail pikachu(
            PokeApiPokemonDto pokemon, PokeApiSpeciesDto species, PokeApiEvolutionChainDto chain) {
        return mapper.toDetail(pokemon, species, chain);
    }

    /** Recorded pikachu species with only the English entries of the given versions kept. */
    private static PokeApiSpeciesDto speciesWithEnglishVersions(List<String> versions) {
        ObjectNode species = PokeApiFixtures.tree("pokemon-species/25.json");
        ArrayNode kept = species.arrayNode();
        for (JsonNode entry : species.get("flavor_text_entries")) {
            boolean english = "en".equals(entry.get("language").get("name").asString());
            if (!english || versions.contains(entry.get("version").get("name").asString())) {
                kept.add(entry);
            }
        }
        species.set("flavor_text_entries", kept);
        return PokeApiFixtures.convert(species, PokeApiSpeciesDto.class);
    }
}
