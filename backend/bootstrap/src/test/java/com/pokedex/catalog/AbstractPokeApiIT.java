package com.pokedex.catalog;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.path.json.config.JsonPathConfig;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Shared harness for the Catalog acceptance tests (US01, later US02).
 *
 * <ul>
 *   <li>Full application on a random port, called over real HTTP with RestAssured.
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
 * <p>Abstract, so Failsafe does not run it on its own.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractPokeApiIT {

    /** Property name the backend must read the PokeAPI base URL from (security.md §4.5). */
    static final String POKEAPI_BASE_URL_PROPERTY = "pokeapi.base-url";

    static final String API_PREFIX = "/api/v2";
    static final String CATALOG_PATH = "/api/v1/pokemon";
    static final String PROBLEM_JSON = "application/problem+json";

    /** Recorded fixtures live in the infrastructure module (owned by the test engineer). */
    private static final Path FIXTURES = Path.of(System.getProperty("basedir", System.getProperty("user.dir")))
            .resolve("../infrastructure/src/test/resources/pokeapi")
            .normalize();

    @RegisterExtension
    static final WireMockExtension POKEAPI = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .failOnUnmatchedRequests(false)
            .build();

    @DynamicPropertySource
    static void pointPokeApiAtWireMock(DynamicPropertyRegistry registry) {
        registry.add(POKEAPI_BASE_URL_PROPERTY, () -> POKEAPI.baseUrl() + API_PREFIX);
    }

    @Value("${local.server.port}")
    int port;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port = port;
        RestAssured.basePath = "";
        // Numbers as BigDecimal so mass assertions (6.9 kg) are exact, never float-rounded by the test itself.
        RestAssured.config = RestAssuredConfig.config()
                .jsonConfig(JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
    }

    // ---------------------------------------------------------------- fixtures

    static String fixture(String relativePath) {
        Path file = FIXTURES.resolve(relativePath);
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Missing PokeAPI fixture " + file, e);
        }
    }

    // ---------------------------------------------------------------- stubs

    /** {@code GET /api/v2/pokemon?offset=&limit=} (trailing slash tolerated). */
    static void stubList(int offset, int limit, ResponseDefinitionBuilder response) {
        POKEAPI.stubFor(get(urlPathMatching(API_PREFIX + "/pokemon/?"))
                .withQueryParam("offset", equalTo(String.valueOf(offset)))
                .withQueryParam("limit", equalTo(String.valueOf(limit)))
                .willReturn(response));
    }

    /** Serves the recorded list page {@code pokemon-list/offset-<o>-limit-<l>.json} for the same query. */
    static void stubRecordedList(int offset, int limit) {
        stubList(offset, limit, okJson(fixture("pokemon-list/offset-%d-limit-%d.json".formatted(offset, limit))));
    }

    /** {@code GET /api/v2/pokemon/{id}} (trailing slash tolerated). */
    static void stubPokemon(int id, ResponseDefinitionBuilder response) {
        POKEAPI.stubFor(
                get(urlPathMatching(API_PREFIX + "/pokemon/" + id + "/?")).willReturn(response));
    }

    static void stubRecordedPokemon(int id) {
        stubPokemon(id, okJson(fixture("pokemon/%d.json".formatted(id))));
    }

    /** {@code GET /api/v2/pokemon-species/{id}} (trailing slash tolerated). */
    static void stubSpecies(int id, ResponseDefinitionBuilder response) {
        POKEAPI.stubFor(get(urlPathMatching(API_PREFIX + "/pokemon-species/" + id + "/?"))
                .willReturn(response));
    }

    static void stubRecordedSpecies(int id) {
        stubSpecies(id, okJson(fixture("pokemon-species/%d.json".formatted(id))));
    }

    static ResponseDefinitionBuilder status(int code) {
        return aResponse()
                .withStatus(code)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"detail\":\"upstream failure " + code + "\"}");
    }
}
