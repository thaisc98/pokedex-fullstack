package com.pokedex.catalog;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.pokedex.support.AbstractIntegrationIT;
import io.restassured.response.Response;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Shared harness for the Catalog acceptance tests (US01, US02). Extends {@link AbstractIntegrationIT} (random port,
 * RestAssured, PostgreSQL Testcontainer, test {@code JWT_SECRET}, cache clear before each test).
 *
 * <ul>
 *   <li>PokeAPI is replaced by an in-process WireMock server. The application is pointed at it through
 *       {@value #POKEAPI_BASE_URL_PROPERTY} (security.md §4.5: the base URL comes from config only). The
 *       value includes the {@code /api/v2} prefix, exactly like the real {@code https://pokeapi.co/api/v2}.
 *   <li>Stub bodies are the real PokeAPI responses recorded under
 *       {@code backend/infrastructure/src/test/resources/pokeapi/} (testing-strategy.md §4). No live network.
 * </ul>
 *
 * <p>Every recorded response contains absolute {@code https://pokeapi.co/...} URLs. The app must never follow
 * them (security.md §4.5): it extracts the id and rebuilds the URL against the configured base. Tests verify
 * that WireMock received the rebuilt requests, so following an upstream URL fails the test even when the
 * machine happens to have internet access.
 *
 * <p><b>Isolation model.</b> All subclasses share <em>one</em> Spring application context: the
 * {@code @DynamicPropertySource} below is inherited, so the context cache key is identical. That is correct
 * because there is also exactly one WireMock server per JVM ({@link #POKEAPI}), started once in a static
 * initializer on a dynamic port and never stopped by the test lifecycle (it dies with the JVM), so the base URL
 * baked into the cached context never goes stale. Tests stay independent because, before every test:
 *
 * <ol>
 *   <li>{@code POKEAPI.resetAll()} drops every stub and the request journal;
 *   <li>every cache of every {@code CacheManager} bean is cleared ({@link AbstractIntegrationIT}).
 * </ol>
 *
 * Tests still use unique (page, size) pairs and Pokémon ids as a second line of defence.
 *
 * <p>Abstract, so Failsafe does not run it on its own.
 */
public abstract class AbstractPokeApiIT extends AbstractIntegrationIT {

    /** Property name the backend must read the PokeAPI base URL from (security.md §4.5). */
    public static final String POKEAPI_BASE_URL_PROPERTY = "pokeapi.base-url";

    public static final String API_PREFIX = "/api/v2";
    public static final String CATALOG_PATH = "/api/v1/pokemon";

    /** Recorded fixtures live in the infrastructure module (owned by the test engineer). */
    private static final Path FIXTURES = Path.of(System.getProperty("basedir", System.getProperty("user.dir")))
            .resolve("../infrastructure/src/test/resources/pokeapi")
            .normalize();

    /** One PokeAPI stand-in for the whole JVM; its port never changes, so a shared context stays valid. */
    public static final WireMockServer POKEAPI =
            new WireMockServer(wireMockConfig().dynamicPort());

    static {
        POKEAPI.start();
    }

    @DynamicPropertySource
    public static void pointPokeApiAtWireMock(DynamicPropertyRegistry registry) {
        registry.add(POKEAPI_BASE_URL_PROPERTY, () -> POKEAPI.baseUrl() + API_PREFIX);
    }

    /** Runs after {@link AbstractIntegrationIT}'s cache clear and RestAssured setup (superclass first). */
    @BeforeEach
    void resetPokeApi() {
        POKEAPI.resetAll();
    }

    // ---------------------------------------------------------------- fixtures

    public static String fixture(String relativePath) {
        Path file = FIXTURES.resolve(relativePath);
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Missing PokeAPI fixture " + file, e);
        }
    }

    // ---------------------------------------------------------------- stubs

    /** {@code GET /api/v2/pokemon?offset=&limit=} (trailing slash tolerated). */
    public static void stubList(int offset, int limit, ResponseDefinitionBuilder response) {
        POKEAPI.stubFor(get(urlPathMatching(API_PREFIX + "/pokemon/?"))
                .withQueryParam("offset", WireMock.equalTo(String.valueOf(offset)))
                .withQueryParam("limit", WireMock.equalTo(String.valueOf(limit)))
                .willReturn(response));
    }

    /** Serves the recorded list page {@code pokemon-list/offset-<o>-limit-<l>.json} for the same query. */
    public static void stubRecordedList(int offset, int limit) {
        stubList(offset, limit, okJson(fixture("pokemon-list/offset-%d-limit-%d.json".formatted(offset, limit))));
    }

    /** {@code GET /api/v2/pokemon/{id}} (trailing slash tolerated). */
    public static void stubPokemon(int id, ResponseDefinitionBuilder response) {
        POKEAPI.stubFor(
                get(urlPathMatching(API_PREFIX + "/pokemon/" + id + "/?")).willReturn(response));
    }

    public static void stubRecordedPokemon(int id) {
        stubPokemon(id, okJson(fixture("pokemon/%d.json".formatted(id))));
    }

    /** {@code GET /api/v2/pokemon-species/{id}} (trailing slash tolerated). */
    public static void stubSpecies(int id, ResponseDefinitionBuilder response) {
        POKEAPI.stubFor(get(urlPathMatching(API_PREFIX + "/pokemon-species/" + id + "/?"))
                .willReturn(response));
    }

    public static void stubRecordedSpecies(int id) {
        stubSpecies(id, okJson(fixture("pokemon-species/%d.json".formatted(id))));
    }

    /**
     * {@code GET /api/v2/pokemon/{name}} (trailing slash tolerated). PokeAPI accepts a name or an id here; the
     * app may call it with the normalised name after it passed {@code PokemonName} validation (security.md §4.5).
     */
    public static void stubPokemonByName(String name, ResponseDefinitionBuilder response) {
        POKEAPI.stubFor(
                get(urlPathMatching(API_PREFIX + "/pokemon/" + name + "/?")).willReturn(response));
    }

    /** {@code GET /api/v2/evolution-chain/{id}} (trailing slash tolerated). */
    public static void stubEvolutionChain(int id, ResponseDefinitionBuilder response) {
        POKEAPI.stubFor(get(urlPathMatching(API_PREFIX + "/evolution-chain/" + id + "/?"))
                .willReturn(response));
    }

    public static void stubRecordedEvolutionChain(int id) {
        stubEvolutionChain(id, okJson(fixture("evolution-chain/%d.json".formatted(id))));
    }

    /** PokeAPI's real 404: {@code 404 application/json {"status":404,"message":"Not Found"}}. */
    public static ResponseDefinitionBuilder pokeApiNotFound() {
        return aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withBody(fixture("errors/not-found.json"));
    }

    public static ResponseDefinitionBuilder status(int code) {
        return aResponse()
                .withStatus(code)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"detail\":\"upstream failure " + code + "\"}");
    }

    // ---------------------------------------------------------------- requests and problem assertions

    public static final String UPSTREAM_UNAVAILABLE_TYPE = "/problems/upstream-unavailable";
    public static final String VALIDATION_ERROR_TYPE = "/problems/validation-error";

    public static Response requestPage(int page, int size) {
        return given().queryParam("page", page)
                .queryParam("size", size)
                .when()
                .get(CATALOG_PATH)
                .then()
                .extract()
                .response();
    }

    /**
     * 503 {@code ProblemDetail} with the exact {@code type} {@value #UPSTREAM_UNAVAILABLE_TYPE}, and no stack
     * trace, exception name, internal topology or partial page in the body (security.md §4.6, architecture §1.5).
     */
    public static void assertUpstreamUnavailableProblem(Response response) {
        response.then()
                .statusCode(503)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo(UPSTREAM_UNAVAILABLE_TYPE))
                .body("status", equalTo(503))
                .body("title", not(blankOrNullString()));

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
