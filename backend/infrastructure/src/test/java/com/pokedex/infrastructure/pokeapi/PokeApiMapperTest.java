package com.pokedex.infrastructure.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Hand-written PokeAPI DTO → domain mapping, driven by the recorded fixtures. */
class PokeApiMapperTest {

    private final PokeApiMapper mapper = new PokeApiMapper();

    @Test
    void should_map_bulbasaur_to_a_summary() {
        PokemonSummary summary = mapper.toSummary(PokeApiFixtures.pokemon(1), PokeApiFixtures.species(1));

        assertThat(summary.id().value()).isEqualTo(1);
        assertThat(summary.name().value()).isEqualTo("bulbasaur");
        assertThat(summary.sprite().value())
                .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png");
        assertThat(summary.category().value()).isEqualTo("Seed Pokémon");
        assertThat(summary.mass().kilograms()).isEqualTo(6.9);
        assertThat(summary.abilities())
                .containsExactly(new Ability("overgrow", false), new Ability("chlorophyll", true));
    }

    @Test
    void should_use_the_placeholder_when_front_default_is_null() {
        PokemonSummary summary = mapper.toSummary(PokeApiFixtures.pokemon(10264), PokeApiFixtures.species(1007));

        assertThat(summary.sprite()).isEqualTo(ImageUrl.PLACEHOLDER);
        assertThat(summary.name().value()).isEqualTo("koraidon-limited-build");
        assertThat(summary.category().value()).isEqualTo("Paradox Pokémon");
        assertThat(summary.mass().kilograms()).isEqualTo(303.0);
    }

    @Test
    void should_use_the_placeholder_when_the_sprite_host_is_not_allowed() {
        PokeApiPokemonDto pokemon = PokeApiFixtures.parse(
                pokemonJson("\"https://evil.example/x.png\"", "69", "bulbasaur"), PokeApiPokemonDto.class);

        assertThat(mapper.toSummary(pokemon, PokeApiFixtures.species(1)).sprite())
                .isEqualTo(ImageUrl.PLACEHOLDER);
    }

    @Test
    void should_use_the_placeholder_when_sprites_are_missing() {
        PokeApiPokemonDto pokemon = PokeApiFixtures.parse("""
                {"id": 1, "name": "bulbasaur", "weight": 69, "abilities": [],
                 "species": {"name": "bulbasaur", "url": "https://pokeapi.co/api/v2/pokemon-species/1/"}}
                """, PokeApiPokemonDto.class);

        assertThat(mapper.toSummary(pokemon, PokeApiFixtures.species(1)).sprite())
                .isEqualTo(ImageUrl.PLACEHOLDER);
    }

    @Test
    void should_fall_back_to_unknown_category_without_an_english_genus() {
        PokeApiSpeciesDto species = PokeApiFixtures.parse("""
                {"id": 1, "genera": [{"genus": "Pokémon Graine", "language": {"name": "fr", "url": "x"}}]}
                """, PokeApiSpeciesDto.class);

        assertThat(mapper.toSummary(PokeApiFixtures.pokemon(1), species).category())
                .isSameAs(Category.UNKNOWN);
    }

    @Test
    void should_fall_back_to_unknown_category_when_genera_are_missing() {
        PokeApiSpeciesDto species = PokeApiFixtures.parse("{\"id\": 1}", PokeApiSpeciesDto.class);

        assertThat(mapper.toSummary(PokeApiFixtures.pokemon(1), species).category())
                .isSameAs(Category.UNKNOWN);
    }

    @Test
    void should_reject_an_upstream_name_that_breaks_the_domain_rules() {
        PokeApiPokemonDto pokemon =
                PokeApiFixtures.parse(pokemonJson("null", "69", "<script>"), PokeApiPokemonDto.class);

        assertThatThrownBy(() -> mapper.toSummary(pokemon, PokeApiFixtures.species(1)))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void should_reject_a_missing_weight() {
        PokeApiPokemonDto pokemon =
                PokeApiFixtures.parse(pokemonJson("null", "null", "bulbasaur"), PokeApiPokemonDto.class);

        assertThatThrownBy(() -> mapper.toSummary(pokemon, PokeApiFixtures.species(1)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_ignore_unknown_upstream_fields() {
        PokeApiPokemonDto pokemon = PokeApiFixtures.parse(
                pokemonJson("null", "69", "bulbasaur").replaceFirst("\\{", "{\"brand_new_field\": [1, 2],"),
                PokeApiPokemonDto.class);

        assertThat(pokemon.name()).isEqualTo("bulbasaur");
    }

    // ------------------------------------------------------------ id extraction (security.md §4.5)

    @Test
    void should_extract_the_pokemon_id_from_a_list_result_url() {
        assertThat(mapper.pokemonId("https://pokeapi.co/api/v2/pokemon/10264/")).isEqualTo(10264);
        assertThat(mapper.pokemonId("https://pokeapi.co/api/v2/pokemon/25")).isEqualTo(25);
    }

    @Test
    void should_extract_the_species_id_from_the_species_url() {
        assertThat(mapper.speciesId(PokeApiFixtures.pokemon(10264))).isEqualTo(1007);
    }

    @Test
    void should_extract_only_the_id_and_ignore_the_host_of_the_upstream_url() {
        // The id is rebuilt against the configured base; the host in the URL is never used.
        assertThat(mapper.pokemonId("https://evil.example/api/v2/pokemon/7/")).isEqualTo(7);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "https://pokeapi.co/api/v2/pokemon/0/",
                "https://pokeapi.co/api/v2/pokemon/-1/",
                "https://pokeapi.co/api/v2/pokemon/abc/",
                "https://pokeapi.co/api/v2/pokemon/12345678/",
                "https://pokeapi.co/api/v2/pokemon-species/1/",
                "https://pokeapi.co/api/v2/pokemon/1/../../admin",
                "https://pokeapi.co/api/v2/pokemon/1/?next=https://evil.example",
                "https://pokeapi.co/api/v2/pokemon/1/#x",
                "https://pokeapi.co/api/v2/pokemon/1 /",
                "ftp://pokeapi.co/api/v2/pokemon/1/",
                "/api/v2/pokemon/1/",
                "pokemon/1"
            })
    void should_reject_an_upstream_url_that_does_not_match_the_strict_pattern(String url) {
        assertThatThrownBy(() -> mapper.pokemonId(url)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_a_pokemon_url_where_a_species_url_is_expected() {
        PokeApiPokemonDto pokemon = PokeApiFixtures.parse(
                pokemonJson("null", "69", "bulbasaur").replace("pokemon-species/1/", "pokemon/1/"),
                PokeApiPokemonDto.class);

        assertThatThrownBy(() -> mapper.speciesId(pokemon)).isInstanceOf(IllegalArgumentException.class);
    }

    private static String pokemonJson(String frontDefault, String weight, String name) {
        return """
                {"id": 1, "name": "%s", "weight": %s,
                 "sprites": {"front_default": %s},
                 "abilities": [{"is_hidden": false, "slot": 1, "ability": {"name": "overgrow", "url": "x"}}],
                 "species": {"name": "bulbasaur", "url": "https://pokeapi.co/api/v2/pokemon-species/1/"}}
                """.formatted(name, weight, frontDefault);
    }
}
