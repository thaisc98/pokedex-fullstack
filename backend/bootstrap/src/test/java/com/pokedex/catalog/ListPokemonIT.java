package com.pokedex.catalog;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * US01 acceptance: list Pokémon page by page ({@code GET /api/v1/pokemon}, UC-01).
 *
 * <p>Traceability: architecture.md §0.1 (sprite / category / mass / skills), §1.2 ({@code PageRequest}), §1.3
 * BR-01 and BR-02, §1.5 (per-page fan-out); openapi.yaml {@code listPokemon}, {@code PageResponse},
 * {@code PokemonSummaryDto}, {@code ProblemDetail}; testing-strategy.md §5 "Catalog / PokeAPI"; security.md §3
 * (catalog is public) and §4.5 (never follow upstream URLs).
 *
 * <p>Every test uses its own (page, size) pair and its own Pokémon ids, so the Caffeine cache (ADR 0004)
 * shared by the Spring context can never make one test depend on another.
 */
@DisplayName("US01 - list Pokémon page by page")
class ListPokemonIT extends AbstractPokeApiIT {

    // ------------------------------------------------------------ happy path

    @Test
    void should_return_page_of_summaries_built_from_pokeapi_when_page_and_size_are_valid() {
        stubRecordedList(0, 3);
        for (int id = 1; id <= 3; id++) {
            stubRecordedPokemon(id);
            stubRecordedSpecies(id);
        }

        Response response = given().queryParam("page", 0)
                .queryParam("size", 3)
                .when()
                .get(CATALOG_PATH)
                .then()
                .statusCode(200)
                .contentType(startsWith("application/json"))
                // PageResponse envelope
                .body("page", equalTo(0))
                .body("size", equalTo(3))
                .body("totalElements", equalTo(1351)) // `count` of the recorded list page
                .body("totalPages", equalTo(451)) // ceil(1351 / 3)
                // order follows the upstream list
                .body("content.id", contains(1, 2, 3))
                .body("content.name", contains("bulbasaur", "ivysaur", "venusaur"))
                // sprite = sprites.front_default (architecture §0.1)
                .body(
                        "content.sprite",
                        contains(
                                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
                                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/2.png",
                                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/3.png"))
                // category = species genus in English
                .body("content.category", contains("Seed Pokémon", "Seed Pokémon", "Seed Pokémon"))
                // skills = abilities with the hidden flag, upstream order
                .body("content[0].abilities.name", contains("overgrow", "chlorophyll"))
                .body("content[0].abilities.hidden", contains(false, true))
                .extract()
                .response();

        // BR-02: hectograms -> kg, rounded to 0.1. 69 hg -> 6.9, 130 hg -> 13.0, 1000 hg -> 100.0
        JsonPath json = response.jsonPath();
        assertThat(new BigDecimal(json.getString("content[0].mass"))).isEqualByComparingTo("6.9");
        assertThat(new BigDecimal(json.getString("content[1].mass"))).isEqualByComparingTo("13.0");
        assertThat(new BigDecimal(json.getString("content[2].mass"))).isEqualByComparingTo("100.0");
    }

    @Test
    void should_expose_only_the_contract_fields_on_each_summary() {
        stubRecordedList(0, 3);
        for (int id = 1; id <= 3; id++) {
            stubRecordedPokemon(id);
            stubRecordedSpecies(id);
        }

        // PokemonSummaryDto has additionalProperties: false; Ability likewise. Upstream extras
        // (weight in hg, moves, url, slot, is_hidden, ...) must not leak through.
        JsonPath json = given().queryParam("page", 0)
                .queryParam("size", 3)
                .get(CATALOG_PATH)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        assertThat(json.getMap("$").keySet())
                .containsExactlyInAnyOrder("content", "page", "size", "totalElements", "totalPages");
        assertThat(json.getMap("content[0]").keySet())
                .containsExactlyInAnyOrder("id", "name", "sprite", "category", "mass", "abilities");
        assertThat(json.getMap("content[0].abilities[0]").keySet()).containsExactlyInAnyOrder("name", "hidden");
    }

    @Test
    void should_rebuild_upstream_urls_against_configured_base_instead_of_following_them() {
        // The recorded list/pokemon bodies point at https://pokeapi.co/... (security.md §4.5).
        stubRecordedList(0, 3);
        for (int id = 1; id <= 3; id++) {
            stubRecordedPokemon(id);
            stubRecordedSpecies(id);
        }

        given().queryParam("page", 0)
                .queryParam("size", 3)
                .get(CATALOG_PATH)
                .then()
                .statusCode(200);

        // Per-resource caching may legitimately skip a repeat, but each resource must have been fetched
        // from the configured base (WireMock) at least once, by numeric id.
        for (int id = 1; id <= 3; id++) {
            assertThat(POKEAPI.findAll(getRequestedFor(urlPathMatching(API_PREFIX + "/pokemon/" + id + "/?"))))
                    .as("GET /pokemon/%d sent to the configured base", id)
                    .isNotEmpty();
            assertThat(POKEAPI.findAll(getRequestedFor(urlPathMatching(API_PREFIX + "/pokemon-species/" + id + "/?"))))
                    .as("GET /pokemon-species/%d sent to the configured base", id)
                    .isNotEmpty();
        }
    }

    // ------------------------------------------------------------ BR-01 defaults and bounds

    @Test
    void should_default_to_page_0_and_size_20_when_paging_params_are_absent() {
        // Real PokeAPI body (count + empty results) re-mapped to offset=0&limit=20 so the test does not need
        // 40 more fixtures; it only proves which defaults are applied. See pokeapi/README.md.
        stubList(0, 20, okJson(fixture("pokemon-list/offset-2000000-limit-20.json")));

        given().when()
                .get(CATALOG_PATH)
                .then()
                .statusCode(200)
                .body("page", equalTo(0))
                .body("size", equalTo(20));

        POKEAPI.verify(getRequestedFor(urlPathMatching(API_PREFIX + "/pokemon/?"))
                .withQueryParam("offset", WireMock.equalTo("0"))
                .withQueryParam("limit", WireMock.equalTo("20")));
    }

    @Test
    void should_return_empty_page_when_page_is_past_the_end() {
        stubRecordedList(2_000_000, 20); // real PokeAPI response: count 1351, results []

        given().queryParam("page", 100_000)
                .queryParam("size", 20)
                .when()
                .get(CATALOG_PATH)
                .then()
                .statusCode(200)
                .contentType(startsWith("application/json"))
                .body("content", empty())
                .body("page", equalTo(100_000))
                .body("size", equalTo(20))
                .body("totalElements", equalTo(1351))
                .body("totalPages", equalTo(68)); // ceil(1351 / 20)
    }

    @Test
    void should_return_empty_page_without_overflowing_offset_when_page_is_huge() {
        // page * size = 214_748_364_700 overflows an int. It must never reach PokeAPI as a negative offset.
        POKEAPI.stubFor(get(urlPathMatching(API_PREFIX + "/pokemon/?"))
                .withQueryParam("offset", matching("[0-9]+"))
                .withQueryParam("limit", WireMock.equalTo("100"))
                .willReturn(okJson(fixture("pokemon-list/offset-2000000-limit-20.json"))));

        given().queryParam("page", Integer.MAX_VALUE)
                .queryParam("size", 100)
                .when()
                .get(CATALOG_PATH)
                .then()
                .statusCode(200)
                .body("content", empty())
                .body("page", equalTo(Integer.MAX_VALUE))
                .body("size", equalTo(100));

        POKEAPI.verify(
                0,
                getRequestedFor(urlPathMatching(API_PREFIX + "/pokemon/?")).withQueryParam("offset", matching("-.*")));
    }

    @Test
    void should_accept_size_100_as_the_inclusive_upper_bound() {
        stubList(10_000_000, 100, okJson(fixture("pokemon-list/offset-2000000-limit-20.json")));

        // page 100_000 * size 100 = offset 10_000_000: past the end, so no per-item fixtures are needed.
        given().queryParam("page", 100_000)
                .queryParam("size", 100)
                .when()
                .get(CATALOG_PATH)
                .then()
                .statusCode(200)
                .body("size", equalTo(100))
                .body("content", empty());
    }

    @ParameterizedTest(name = "page={0}, size={1} -> 400 on ''{2}''")
    @CsvSource({
        "0,   0,   size",
        "0,   101, size",
        "-1,  20,  page",
        "abc, 20,  page",
        "0,   abc, size",
    })
    void should_reject_invalid_paging_with_validation_problem_detail(String page, String size, String field) {
        int upstreamCallsBefore = POKEAPI.getAllServeEvents().size();

        given().queryParam("page", page)
                .queryParam("size", size)
                .when()
                .get(CATALOG_PATH)
                .then()
                .statusCode(400)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/validation-error"))
                .body("status", equalTo(400))
                .body("title", notNullValue())
                .body("errors", not(empty()))
                .body("errors.field", hasItem(field))
                .body("errors.message", not(hasItem(blankOrNullString())));

        assertThat(POKEAPI.getAllServeEvents())
                .as("invalid paging is rejected before any PokeAPI call")
                .hasSize(upstreamCallsBefore);
    }

    // ------------------------------------------------------------ sprite fallback (testing-strategy §5)

    @Test
    void should_use_placeholder_sprite_when_front_default_is_null() {
        // Real PokeAPI data: koraidon-limited-build (#10264) has sprites.front_default = null, and its species
        // id (1007) differs from its Pokémon id, so the species URL must be rebuilt from species.url.
        stubRecordedList(1288, 1);
        stubRecordedPokemon(10264);
        stubRecordedSpecies(1007);

        Response response = given().queryParam("page", 1288)
                .queryParam("size", 1)
                .when()
                .get(CATALOG_PATH)
                .then()
                .statusCode(200)
                .body("content.id", contains(10264))
                .body("content[0].name", equalTo("koraidon-limited-build"))
                .body("content[0].sprite", notNullValue())
                // ImageUrl: always present, never "null"/blank; an absolute https URL or an app-relative path
                .body("content[0].sprite", matchesPattern("^(https://\\S+|/\\S+)$"))
                .body("content[0].category", equalTo("Paradox Pokémon"))
                .body("content[0].abilities.name", contains("orichalcum-pulse"))
                .body("content[0].abilities.hidden", contains(false))
                .extract()
                .response();

        assertThat(new BigDecimal(response.jsonPath().getString("content[0].mass")))
                .isEqualByComparingTo("303.0"); // 3030 hg
        POKEAPI.verify(getRequestedFor(urlPathMatching(API_PREFIX + "/pokemon-species/1007/?")));
    }

    // ------------------------------------------------------------ cache (ADR 0004, testing-strategy §5)

    @Test
    void should_make_zero_upstream_calls_when_the_same_page_is_requested_twice() {
        stubRecordedList(1, 1);
        stubRecordedPokemon(2);
        stubRecordedSpecies(2);

        given().queryParam("page", 1)
                .queryParam("size", 1)
                .get(CATALOG_PATH)
                .then()
                .statusCode(200);
        int upstreamCallsAfterFirst = POKEAPI.getAllServeEvents().size();

        given().queryParam("page", 1)
                .queryParam("size", 1)
                .get(CATALOG_PATH)
                .then()
                .statusCode(200)
                .body("content.name", contains("ivysaur"));

        assertThat(POKEAPI.getAllServeEvents())
                .as("second identical call is served from the cache")
                .hasSize(upstreamCallsAfterFirst);
    }

    // ------------------------------------------------------------ access (security.md §3.2)

    @Test
    void should_allow_anonymous_access_without_cookie_or_credentials() {
        stubRecordedList(0, 3);
        for (int id = 1; id <= 3; id++) {
            stubRecordedPokemon(id);
            stubRecordedSpecies(id);
        }

        given().when()
                .get(CATALOG_PATH + "?page=0&size=3")
                .then()
                .statusCode(200)
                .header("WWW-Authenticate", nullValue())
                // STATELESS: no server session is ever created (security.md §4.2.1)
                .cookie("JSESSIONID", nullValue());
    }
}
