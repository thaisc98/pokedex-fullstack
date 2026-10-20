package com.pokedex.catalog;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * US01 failure modes: PokeAPI down, erroring or returning garbage ({@code GET /api/v1/pokemon} → 503).
 *
 * <p>Traceability: architecture.md §1.4 UC-01 ("upstream down → 503") and §1.5 ("if an item still fails, the
 * whole page returns 503 ProblemDetail; we do not return half-filled cards"); openapi.yaml
 * {@code UpstreamUnavailable}; testing-strategy.md §5 (429 / 5xx / connection refused → retried then 503,
 * malformed JSON → no stack trace); security.md §4.6 (no stack traces or internal details in errors).
 *
 * <p>Kept in its own class (own Spring context, own cache) and every test uses a unique (page, size) pair and
 * unique Pokémon ids, so nothing cached by a successful call can hide a failure.
 */
@DisplayName("US01 - list Pokémon when PokeAPI fails")
class ListPokemonUpstreamFailureIT extends AbstractPokeApiIT {

    @ParameterizedTest(name = "list endpoint answers HTTP {1} -> 503")
    @CsvSource({
        "40, 500", "41, 502", "42, 503", "43, 504", "44, 429",
    })
    void should_return_503_problem_when_pokeapi_list_endpoint_fails(int page, int upstreamStatus) {
        stubList(page * 3, 3, status(upstreamStatus));

        Response response = requestPage(page, 3);

        assertUpstreamUnavailableProblem(response);
        POKEAPI.verify(getRequestedFor(urlPathMatching(API_PREFIX + "/pokemon/?")));
    }

    @ParameterizedTest(name = "connection fault {1} -> 503")
    @CsvSource({
        "50, CONNECTION_RESET_BY_PEER",
        "51, EMPTY_RESPONSE",
        "52, RANDOM_DATA_THEN_CLOSE",
    })
    void should_return_503_problem_when_pokeapi_is_unreachable(int page, Fault fault) {
        stubList(page * 3, 3, aResponse().withFault(fault));

        assertUpstreamUnavailableProblem(requestPage(page, 3));
    }

    @Test
    void should_return_503_problem_without_stack_trace_when_pokeapi_returns_malformed_json() {
        stubList(
                60 * 3,
                3,
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"count\": 1351, \"results\": [ {\"name\": \"bulba"));

        // openapi.yaml lists only 400 and 503 for listPokemon, so a broken upstream body is a 503.
        assertUpstreamUnavailableProblem(requestPage(60, 3));
    }

    @Test
    void should_return_503_instead_of_partial_page_when_one_pokemon_call_fails() {
        // Real list page with charmander (#4); its /pokemon call keeps failing, its species call succeeds.
        stubRecordedList(3, 1);
        stubPokemon(4, status(500));
        stubRecordedSpecies(4);

        assertUpstreamUnavailableProblem(requestPage(3, 1));
    }

    @Test
    void should_return_503_instead_of_partial_page_when_one_species_call_fails() {
        // Real list page with charmeleon (#5); its /pokemon call succeeds, its species call keeps failing.
        stubRecordedList(4, 1);
        stubRecordedPokemon(5);
        stubSpecies(5, status(503));

        assertUpstreamUnavailableProblem(requestPage(4, 1));
    }

    @Test
    void should_retry_pokeapi_on_5xx_before_giving_up() {
        stubList(70 * 3, 3, status(500));

        assertUpstreamUnavailableProblem(requestPage(70, 3));

        // @Retryable on 5xx/IO (backend/AGENTS.md): more than one attempt reached PokeAPI.
        assertThat(POKEAPI.findAll(getRequestedFor(urlPathMatching(API_PREFIX + "/pokemon/?"))
                        .withQueryParam("offset", WireMock.equalTo("210"))))
                .hasSizeGreaterThan(1);
    }

    // ------------------------------------------------------------ helpers

    private static Response requestPage(int page, int size) {
        return given().queryParam("page", page)
                .queryParam("size", size)
                .when()
                .get(CATALOG_PATH)
                .then()
                .extract()
                .response();
    }

    private static void assertUpstreamUnavailableProblem(Response response) {
        response.then()
                .statusCode(503)
                .contentType(startsWith(PROBLEM_JSON))
                .body("status", equalTo(503))
                .body("title", not(blankOrNullString()))
                .body("type", not(blankOrNullString()));

        // security.md §4.6: no stack traces, exception names or internal topology in the response.
        String body = response.asString();
        assertThat(body)
                .doesNotContain("Exception")
                .doesNotContain("\tat ")
                .doesNotContain("com.pokedex")
                .doesNotContain("tools.jackson")
                .doesNotContain(POKEAPI.baseUrl())
                .doesNotContain("\"content\"");
    }
}
