package com.pokedex.catalog;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * US02 acceptance: Pokémon detail by id or name ({@code GET /api/v1/pokemon/{idOrName}}, UC-02,
 * {@code getPokemonDetail}).
 *
 * <p>Traceability:
 *
 * <ul>
 *   <li>architecture.md §0.1: artwork = {@code sprites.other.official-artwork.front_default}; description =
 *       English flavor text; evolution = {@code /evolution-chain/{id}}.
 *   <li>§1.2 / §1.3: BR-02 hectograms → kg, {@code Height} decimetres → metres, BR-03 description cleanup (latest
 *       game version with English text; {@code \f}, {@code \n}, soft hyphen → space; whitespace collapsed; empty
 *       when there is none), BR-04 branches and stage order.
 *   <li>openapi.yaml: {@code IdOrNameParam} (all digits → id; otherwise lowercased then {@code ^[a-z0-9-]{1,50}$}),
 *       {@code PokemonDetailDto}, {@code EvolutionNode}, {@code EvolutionTrigger} ({@code oneOf} on {@code kind}).
 *   <li>testing-strategy.md §5: {@code 0}, {@code -5}, {@code 99999} → 404; {@code Pikachu} normalised;
 *       {@code mr-mime} valid; {@code pika;drop} → 400; missing artwork → sprite; upstream 404 → our 404.
 *   <li>security.md §3.2 (public) and §4.5 (species and chain URLs are rebuilt from ids, never followed).
 * </ul>
 *
 * <p>Fixtures are real PokeAPI responses (see {@code pokeapi/README.md}). Where a test needs a variant PokeAPI
 * does not have (no official artwork, a single old flavor text, no English text), the real fixture is changed
 * <em>inside the test</em> with Jackson and the change is named in the test.
 */
@DisplayName("US02 - Pokémon detail by id or name")
class GetPokemonDetailIT extends AbstractPokeApiIT {

    static final String DETAIL_PATH = CATALOG_PATH + "/{idOrName}";
    static final String NOT_FOUND_TYPE = "/problems/not-found";
    static final String PLACEHOLDER =
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/poke-ball.png";
    static final String SPRITES = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/";

    static final JsonMapper JSON = JsonMapper.builder().build();

    // ------------------------------------------------------------ happy path: pikachu

    @Test
    void should_return_full_detail_for_pikachu_by_id() {
        stubPikachu();

        Response response = requestDetail("25");

        response.then()
                .statusCode(200)
                .contentType(startsWith("application/json"))
                .body("id", equalTo(25))
                .body("name", equalTo("pikachu"))
                .body("artwork", equalTo(SPRITES + "other/official-artwork/25.png"))
                .body("types", contains("electric"))
                // the six base stats, upstream order
                .body("stats.name", contains("hp", "attack", "defense", "special-attack", "special-defense", "speed"))
                .body("stats.value", contains(35, 55, 40, 50, 50, 90))
                // BR-03: latest version with English text (legends-arceus), \n -> space, whitespace collapsed
                .body(
                        "description",
                        equalTo("Possesses cheek sacs in which it stores electricity. This clever forest-dweller"
                                + " roasts tough berries with an electric shock before consuming them."));

        JsonPath json = response.jsonPath();
        assertThat(json.getMap("$").keySet())
                .as("PokemonDetailDto fields exactly (additionalProperties: false)")
                .containsExactlyInAnyOrder(
                        "id", "name", "artwork", "stats", "description", "evolution", "types", "height", "mass");
        assertThat(json.getMap("stats[0]").keySet()).containsExactlyInAnyOrder("name", "value");
        assertDecimal(json, "height", "0.4"); // 4 dm
        assertDecimal(json, "mass", "6.0"); // 60 hg (BR-02)

        // BR-04: pichu (baby, root, no trigger) -> pikachu (level-up, friendship) -> raichu (thunder-stone)
        assertThat(json.getMap("evolution").keySet()).containsExactly("root");
        assertNode(json, "evolution.root", 172, "pichu");
        assertThat(json.get("evolution.root.trigger") == null)
                .as("root has no trigger")
                .isTrue();
        assertThat(json.getList("evolution.root.evolvesTo")).hasSize(1);
        assertNode(json, "evolution.root.evolvesTo[0]", 25, "pikachu");
        assertLevelUpWithoutMinLevel(json, "evolution.root.evolvesTo[0].trigger");
        assertThat(json.getList("evolution.root.evolvesTo[0].evolvesTo")).hasSize(1);
        assertNode(json, "evolution.root.evolvesTo[0].evolvesTo[0]", 26, "raichu");
        assertUseItem(json, "evolution.root.evolvesTo[0].evolvesTo[0].trigger", "thunder-stone");
        assertThat(json.getList("evolution.root.evolvesTo[0].evolvesTo[0].evolvesTo"))
                .isEmpty();
    }

    @Test
    void should_return_the_same_detail_for_id_name_and_mixed_case_name() {
        stubPikachu();

        Map<String, Object> byId =
                requestDetail("25").then().statusCode(200).extract().jsonPath().getMap("$");
        Map<String, Object> byName = requestDetail("pikachu")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getMap("$");
        Map<String, Object> mixedCase = requestDetail("Pikachu")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getMap("$");

        assertThat(byName).as("by name").isEqualTo(byId);
        assertThat(mixedCase).as("'Pikachu' is normalised, not rejected").isEqualTo(byId);
    }

    @Test
    void should_rebuild_species_and_chain_urls_from_ids_instead_of_following_them() {
        stubPikachu();

        requestDetail("25").then().statusCode(200);

        // The recorded bodies point at https://pokeapi.co/...; the app must call the configured base by id.
        POKEAPI.verify(getRequestedFor(urlPathMatching(API_PREFIX + "/pokemon-species/25/?")));
        POKEAPI.verify(getRequestedFor(urlPathMatching(API_PREFIX + "/evolution-chain/10/?")));
    }

    // ------------------------------------------------------------ eevee: 8 branches (BR-04)

    @Test
    void should_keep_all_eight_eevee_branches_in_upstream_order() {
        stubRecordedPokemon(133);
        stubPokemonByName("eevee", okJson(fixture("pokemon/133.json")));
        stubRecordedSpecies(133);
        stubRecordedEvolutionChain(67);

        Response response = requestDetail("133");

        response.then()
                .statusCode(200)
                .body("name", equalTo("eevee"))
                .body("types", contains("normal"))
                .body("evolution.root.name", equalTo("eevee"))
                .body("evolution.root.speciesId", equalTo(133))
                .body("evolution.root.trigger", nullValue())
                .body(
                        "evolution.root.evolvesTo.name",
                        contains(
                                "vaporeon", "jolteon", "flareon", "espeon", "umbreon", "leafeon", "glaceon", "sylveon"))
                .body("evolution.root.evolvesTo.speciesId", contains(134, 135, 136, 196, 197, 470, 471, 700));

        JsonPath json = response.jsonPath();
        assertDecimal(json, "height", "0.3");
        assertDecimal(json, "mass", "6.5");
        for (int i = 0; i < 8; i++) {
            assertThat(json.getList("evolution.root.evolvesTo[" + i + "].evolvesTo"))
                    .as("branch %d is a final form", i)
                    .isEmpty();
        }
        String branch = "evolution.root.evolvesTo";
        assertUseItem(json, branch + "[0].trigger", "water-stone");
        assertUseItem(json, branch + "[1].trigger", "thunder-stone");
        assertUseItem(json, branch + "[2].trigger", "fire-stone");
        assertLevelUpWithoutMinLevel(json, branch + "[3].trigger"); // espeon: friendship + day
        assertLevelUpWithoutMinLevel(json, branch + "[4].trigger"); // umbreon: friendship + night
        assertLevelUpWithoutMinLevel(json, branch + "[7].trigger"); // sylveon: fairy move + affection
        // Leafeon / Glaceon: PokeAPI lists level-up (special rock) and use-item (leaf/ice stone, is_default).
        // Which detail wins is an open question for the human; either reading is accepted until it is decided.
        assertThat(json.getString(branch + "[5].trigger.kind")).isIn("level-up", "use-item");
        assertThat(json.getString(branch + "[6].trigger.kind")).isIn("level-up", "use-item");
    }

    // ------------------------------------------------------------ mr-mime: hyphenated name, 3 stages, min level

    @Test
    void should_accept_hyphenated_name_mr_mime() {
        stubRecordedPokemon(122);
        stubPokemonByName("mr-mime", okJson(fixture("pokemon/122.json")));
        stubRecordedSpecies(122);
        stubRecordedEvolutionChain(57);

        Response response = requestDetail("mr-mime");

        response.then()
                .statusCode(200)
                .body("id", equalTo(122))
                .body("name", equalTo("mr-mime"))
                .body("types", contains("psychic", "fairy"))
                .body(
                        "description",
                        equalTo("The behavior of this clown-like Pokémon reminds one of pantomime. It creates"
                                + " invisible walls using a force emitted from its fingertips."));

        JsonPath json = response.jsonPath();
        assertDecimal(json, "height", "1.3");
        assertDecimal(json, "mass", "54.5");
        assertNode(json, "evolution.root", 439, "mime-jr");
        assertNode(json, "evolution.root.evolvesTo[0]", 122, "mr-mime");
        assertLevelUpWithoutMinLevel(json, "evolution.root.evolvesTo[0].trigger"); // knows Mimic
        assertNode(json, "evolution.root.evolvesTo[0].evolvesTo[0]", 866, "mr-rime");
        assertThat(json.getMap("evolution.root.evolvesTo[0].evolvesTo[0].trigger"))
                .containsOnlyKeys("kind", "minLevel")
                .containsEntry("kind", "level-up")
                .containsEntry("minLevel", 42);
    }

    // ------------------------------------------------------------ images (testing-strategy §5)

    @Test
    void should_fall_back_to_sprite_when_official_artwork_is_missing() {
        // Edited in-test: real pikachu with sprites.other.official-artwork.front_default set to null.
        // (No real Pokémon with a sprite but no official artwork was found; see pokeapi/README.md.)
        ObjectNode pokemon = (ObjectNode) JSON.readTree(fixture("pokemon/25.json"));
        ((ObjectNode) pokemon.get("sprites").get("other").get("official-artwork")).putNull("front_default");
        stubPokemon(25, okJson(JSON.writeValueAsString(pokemon)));
        stubRecordedSpecies(25);
        stubRecordedEvolutionChain(10);

        requestDetail("25").then().statusCode(200).body("artwork", equalTo(SPRITES + "25.png"));
    }

    @Test
    void should_use_placeholder_artwork_when_artwork_and_sprite_are_both_missing() {
        // Real data: koraidon-limited-build (#10264) has neither official artwork nor front_default;
        // species 1007 differs from the Pokémon id; chain 535 is a single stage.
        stubRecordedPokemon(10264);
        stubRecordedSpecies(1007);
        stubRecordedEvolutionChain(535);

        Response response = requestDetail("10264");

        response.then()
                .statusCode(200)
                .body("name", equalTo("koraidon-limited-build"))
                .body("artwork", equalTo(PLACEHOLDER))
                .body("types", contains("fighting", "dragon"))
                .body("evolution.root.name", equalTo("koraidon"))
                .body("evolution.root.speciesId", equalTo(1007))
                .body("evolution.root.evolvesTo", empty());
        assertDecimal(response.jsonPath(), "height", "3.5");
        assertDecimal(response.jsonPath(), "mass", "303.0");
        POKEAPI.verify(getRequestedFor(urlPathMatching(API_PREFIX + "/pokemon-species/1007/?")));
    }

    // ------------------------------------------------------------ description (BR-03)

    @Test
    void should_clean_form_feeds_and_newlines_in_description() {
        // Edited in-test: real pikachu species keeping only the English "red" entry, which contains \n and \f.
        stubRecordedPokemon(25);
        stubSpecies(25, okJson(speciesWithEnglishEntries("pokemon-species/25.json", List.of("red"))));
        stubRecordedEvolutionChain(10);

        requestDetail("25")
                .then()
                .statusCode(200)
                .body(
                        "description",
                        equalTo("When several of these POKéMON gather, their electricity could build and cause"
                                + " lightning storms."));
    }

    @Test
    void should_return_empty_description_when_species_has_no_english_flavor_text() {
        // Edited in-test: real pikachu species with every English flavor text removed.
        stubRecordedPokemon(25);
        stubSpecies(25, okJson(speciesWithEnglishEntries("pokemon-species/25.json", List.of())));
        stubRecordedEvolutionChain(10);

        requestDetail("25").then().statusCode(200).body("description", equalTo(""));
    }

    // ------------------------------------------------------------ 404 (testing-strategy §5)

    @ParameterizedTest(name = "''{0}'' -> 404 /problems/not-found")
    @ValueSource(strings = {"99999", "0", "-5", "missingno"})
    void should_return_not_found_problem_when_pokemon_does_not_exist(String idOrName) {
        // PokeAPI's real 404 for whatever the app may ask for; `0` and `-5` may also be decided locally.
        stubPokemonByName(idOrName, pokeApiNotFound());

        requestDetail(idOrName)
                .then()
                .statusCode(404)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo(NOT_FOUND_TYPE))
                .body("status", equalTo(404))
                .body("title", not(blankOrNullString()));
    }

    // ------------------------------------------------------------ 400 (IdOrNameParam)

    @Test
    void should_reject_name_with_semicolon_with_bad_request_problem() {
        int before = POKEAPI.getAllServeEvents().size();

        // Literal path, no client-side encoding: exactly what testing-strategy §5 lists.
        given().urlEncodingEnabled(false)
                .when()
                .get(CATALOG_PATH + "/pika;drop")
                .then()
                .statusCode(400)
                .contentType(startsWith(PROBLEM_JSON))
                .body("status", equalTo(400));

        assertThat(POKEAPI.getAllServeEvents()).as("no PokeAPI call").hasSize(before);
    }

    @ParameterizedTest(name = "''{0}'' -> 400 /problems/validation-error")
    @ValueSource(
            strings = {
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", // 51 chars
                "pika_chu",
                "pika.chu",
                "pikachu!",
            })
    void should_reject_malformed_name_with_validation_problem(String idOrName) {
        assertThat(idOrName.length() == 51 || !idOrName.matches("[A-Za-z0-9-]{1,50}"))
                .as("test data is really malformed")
                .isTrue();
        int before = POKEAPI.getAllServeEvents().size();

        requestDetail(idOrName)
                .then()
                .statusCode(400)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo(VALIDATION_ERROR_TYPE))
                .body("errors.field", hasItem("idOrName"));

        assertThat(POKEAPI.getAllServeEvents()).as("no PokeAPI call").hasSize(before);
    }

    // ------------------------------------------------------------ access and cache

    @Test
    void should_allow_anonymous_access_to_detail() {
        stubPikachu();

        Response response = requestDetail("25")
                .then()
                .statusCode(200)
                .header("WWW-Authenticate", nullValue())
                .extract()
                .response();

        assertThat(response.getCookies()).doesNotContainKey("JSESSIONID");
        assertThat(response.getHeaders().getValues("Set-Cookie"))
                .noneSatisfy(c -> assertThat(c).containsIgnoringCase("JSESSIONID"));
    }

    @Test
    void should_make_zero_upstream_calls_when_the_same_detail_is_requested_twice() {
        stubPikachu();

        requestDetail("25").then().statusCode(200);
        int afterFirst = POKEAPI.getAllServeEvents().size();
        requestDetail("25").then().statusCode(200).body("name", equalTo("pikachu"));

        assertThat(POKEAPI.getAllServeEvents()).as("served from cache").hasSize(afterFirst);
    }

    @Test
    void should_reuse_pokemon_and_species_cached_by_the_list_when_detail_is_requested() {
        // ADR 0004: each PokeAPI resource is cached on its own, so the list's /pokemon/1 and
        // /pokemon-species/1 are reused by the detail. Only the evolution chain is new.
        stubRecordedList(0, 3);
        for (int id = 1; id <= 3; id++) {
            stubRecordedPokemon(id);
            stubRecordedSpecies(id);
        }
        stubPokemonByName("bulbasaur", okJson(fixture("pokemon/1.json")));
        stubRecordedEvolutionChain(1);

        requestPage(0, 3).then().statusCode(200);
        int pokemonCalls = countRequests(API_PREFIX + "/pokemon/(1|bulbasaur)/?");
        int speciesCalls = countRequests(API_PREFIX + "/pokemon-species/1/?");

        requestDetail("1").then().statusCode(200).body("name", equalTo("bulbasaur"));

        assertThat(countRequests(API_PREFIX + "/pokemon/(1|bulbasaur)/?"))
                .as("/pokemon/1 reused from the cache")
                .isEqualTo(pokemonCalls);
        assertThat(countRequests(API_PREFIX + "/pokemon-species/1/?"))
                .as("/pokemon-species/1 reused from the cache")
                .isEqualTo(speciesCalls);
        assertThat(countRequests(API_PREFIX + "/evolution-chain/1/?")).isEqualTo(1);
    }

    // ------------------------------------------------------------ helpers

    static void stubPikachu() {
        stubRecordedPokemon(25);
        stubPokemonByName("pikachu", okJson(fixture("pokemon/25.json")));
        stubRecordedSpecies(25);
        stubRecordedEvolutionChain(10);
    }

    static Response requestDetail(String idOrName) {
        return given().when().get(DETAIL_PATH, idOrName).then().extract().response();
    }

    static int countRequests(String pathRegex) {
        return POKEAPI.findAll(getRequestedFor(urlPathMatching(pathRegex))).size();
    }

    static void assertDecimal(JsonPath json, String path, String expected) {
        assertThat(new BigDecimal(json.getString(path))).as(path).isEqualByComparingTo(expected);
    }

    static void assertNode(JsonPath json, String path, int speciesId, String name) {
        assertThat(json.getMap(path).keySet())
                .as("%s fields (EvolutionNode)", path)
                .contains("speciesId", "name", "evolvesTo")
                .isSubsetOf("speciesId", "name", "trigger", "evolvesTo");
        assertThat(json.getInt(path + ".speciesId")).as(path + ".speciesId").isEqualTo(speciesId);
        assertThat(json.getString(path + ".name")).as(path + ".name").isEqualTo(name);
    }

    /** {@code {"kind":"level-up"}}; {@code minLevel} is an optional integer, so it is absent, never null. */
    static void assertLevelUpWithoutMinLevel(JsonPath json, String path) {
        assertThat(json.getMap(path)).as(path).containsOnlyKeys("kind").containsEntry("kind", "level-up");
    }

    static void assertUseItem(JsonPath json, String path, String item) {
        assertThat(json.getMap(path))
                .as(path)
                .containsOnlyKeys("kind", "item")
                .containsEntry("kind", "use-item")
                .containsEntry("item", item);
    }

    /** Real species fixture with only the English flavor texts of the given versions kept. */
    static String speciesWithEnglishEntries(String fixturePath, List<String> keepVersions) {
        ObjectNode species = (ObjectNode) JSON.readTree(fixture(fixturePath));
        ArrayNode kept = JSON.createArrayNode();
        for (JsonNode entry : species.get("flavor_text_entries")) {
            boolean english = "en".equals(entry.get("language").get("name").asString());
            if (!english
                    || keepVersions.contains(entry.get("version").get("name").asString())) {
                kept.add(entry);
            }
        }
        species.set("flavor_text_entries", kept);
        return JSON.writeValueAsString(species);
    }
}
