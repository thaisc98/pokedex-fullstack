package com.pokedex.catalog;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Name search: {@code GET /api/v1/pokemon/search?q=&limit=} (public).
 *
 * <p>Contract (lead decision, openapi.yaml to follow): {@code q} is trimmed, lowercased, spaces → hyphens, then
 * must match {@code ^[a-z0-9-]{1,50}$} (else 400 {@code validation-error}, field {@code q}); {@code limit} defaults
 * to 10 and must be 1–20 (else 400, field {@code limit}). The body is {@code {"results":[{"id","name"}]}}: names that
 * START with q first, then names that CONTAIN q, each group ordered by id, at most {@code limit}. The names come from
 * ONE upstream call {@code GET /api/v2/pokemon?offset=0&limit=100000}, cached 24 h. Upstream failure → 503.
 *
 * <p>The upstream answer is the real, complete PokeAPI response {@code pokemon-list/offset-0-limit-100000.json}
 * (1351 entries, recorded 2026-10-22), so the expected results below are what real data gives.
 */
@DisplayName("Catalog - search by name")
class SearchPokemonIT extends AbstractPokeApiIT {

    static final String SEARCH_PATH = CATALOG_PATH + "/search";

    static void stubFullNameList() {
        stubList(0, 100_000, okJson(fixture("pokemon-list/offset-0-limit-100000.json")));
    }

    static Response search(String q, Integer limit) {
        var request = given();
        if (q != null) {
            request = request.queryParam("q", q);
        }
        if (limit != null) {
            request = request.queryParam("limit", limit);
        }
        return request.when().get(SEARCH_PATH).then().extract().response();
    }

    @Test
    void should_list_prefix_matches_before_contains_matches_each_ordered_by_id() {
        stubFullNameList();

        // Real data: ducklett (#580) starts with "duck"; psyduck (#54) and golduck (#55) only contain it.
        Response response = search("duck", null);

        response.then()
                .statusCode(200)
                .contentType(startsWith("application/json"))
                .body("results.name", contains("ducklett", "psyduck", "golduck"))
                .body("results.id", contains(580, 54, 55));
        JsonPath json = response.jsonPath();
        assertThat(json.getMap("$").keySet()).containsExactly("results");
        assertThat(json.getMap("results[0]").keySet()).containsExactlyInAnyOrder("id", "name");
        POKEAPI.verify(
                1,
                getRequestedFor(urlPathMatching(API_PREFIX + "/pokemon/?"))
                        .withQueryParam("offset", WireMock.equalTo("0"))
                        .withQueryParam("limit", WireMock.equalTo("100000")));
    }

    @Test
    void should_normalize_case_and_spaces_in_q() {
        stubFullNameList();

        search("Pika", null)
                .then()
                .statusCode(200)
                .body("results", hasSize(10)) // default limit; 17 names start with "pika"
                .body("results[0].name", equalTo("pikachu"))
                .body("results[0].id", equalTo(25));
        search("  Mr Mime ", null)
                .then()
                .statusCode(200)
                .body("results.name", contains("mr-mime", "mr-mime-galar"))
                .body("results.id", contains(122, 10168));
    }

    @Test
    void should_return_at_most_limit_results() {
        stubFullNameList();

        search("pika", 3).then().statusCode(200).body("results.id", contains(25, 10080, 10081));
    }

    @Test
    void should_return_an_empty_list_when_nothing_matches() {
        stubFullNameList();

        search("zzzz", null).then().statusCode(200).body("results", empty());
    }

    @ParameterizedTest(name = "q={0}, limit={1} -> 400 on ''{2}''")
    @CsvSource(
            value = {
                "pika;drop, NULL, q",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa, NULL, q", // 51 chars
                "'', NULL, q",
                "NULL, NULL, q",
                "pika, 0, limit",
                "pika, 21, limit",
            },
            nullValues = "NULL")
    void should_reject_invalid_q_or_limit_with_validation_problem(String q, Integer limit, String field) {
        int before = POKEAPI.getAllServeEvents().size();

        search(q, limit)
                .then()
                .statusCode(400)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo(VALIDATION_ERROR_TYPE))
                .body("errors.field", hasItem(field));

        assertThat(POKEAPI.getAllServeEvents())
                .as("rejected before any PokeAPI call")
                .hasSize(before);
    }

    @Test
    void should_return_503_when_pokeapi_fails() {
        stubList(0, 100_000, status(500));

        assertUpstreamUnavailableProblem(search("pika", null));
    }

    @Test
    void should_make_zero_upstream_calls_for_a_second_search() {
        stubFullNameList();
        search("pika", null).then().statusCode(200);
        int afterFirst = POKEAPI.getAllServeEvents().size();

        search("duck", null).then().statusCode(200).body("results.name", contains("ducklett", "psyduck", "golduck"));

        assertThat(POKEAPI.getAllServeEvents())
                .as("name list served from the cache")
                .hasSize(afterFirst);
    }

    @Test
    void should_allow_anonymous_search() {
        stubFullNameList();

        search("pika", 1)
                .then()
                .statusCode(200)
                .header("WWW-Authenticate", nullValue())
                .body("results.name", contains("pikachu"));
    }
}
