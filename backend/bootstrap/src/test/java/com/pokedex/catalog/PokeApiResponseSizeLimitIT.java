package com.pokedex.catalog;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * security.md §4.5: PokeAPI responses are capped at <b>1 MB</b>. A bigger body is treated as an upstream failure
 * (503 {@code ProblemDetail}, never a partial page, architecture §1.5); a body under the cap is accepted.
 *
 * <p>The oversized bodies are built here from real recorded fixtures by adding one unknown {@code _padding}
 * string member. The JSON stays valid and the upstream DTOs ignore unknown fields (testing-strategy §5), so the
 * only thing that changes is the size. MB-sized files are never committed.
 *
 * <p>"1 MB" may be read as 1,000,000 or 1,048,576 bytes. The two sizes used here sit outside that range on both
 * sides, so the tests hold for either reading.
 */
@DisplayName("US01 hardening - PokeAPI response size limit")
class PokeApiResponseSizeLimitIT extends AbstractPokeApiIT {

    /** Above 1 MB under either reading (1,048,576 B). */
    static final int OVER_LIMIT_BYTES = 1_200_000;

    /** Below 1 MB under either reading (1,000,000 B). */
    static final int UNDER_LIMIT_BYTES = 900_000;

    @Test
    void should_return_503_without_partial_page_when_one_pokemon_response_exceeds_1_mb() {
        stubRecordedList(0, 3);
        for (int id = 1; id <= 3; id++) {
            stubRecordedSpecies(id);
        }
        stubRecordedPokemon(1);
        stubPokemon(2, okJson(paddedTo(fixture("pokemon/2.json"), OVER_LIMIT_BYTES)));
        stubRecordedPokemon(3);

        assertUpstreamUnavailableProblem(requestPage(0, 3));
    }

    @Test
    void should_return_503_when_the_list_response_exceeds_1_mb() {
        stubList(1, 1, okJson(paddedTo(fixture("pokemon-list/offset-1-limit-1.json"), OVER_LIMIT_BYTES)));
        stubRecordedPokemon(2);
        stubRecordedSpecies(2);

        assertUpstreamUnavailableProblem(requestPage(1, 1));
    }

    @Test
    void should_accept_pokemon_response_just_under_1_mb() {
        stubRecordedList(1, 1);
        stubPokemon(2, okJson(paddedTo(fixture("pokemon/2.json"), UNDER_LIMIT_BYTES)));
        stubRecordedSpecies(2);

        requestPage(1, 1)
                .then()
                .statusCode(200)
                .body("content.id", contains(2))
                .body("content.name", contains("ivysaur"));
    }

    // ------------------------------------------------------------ helpers

    /** Inserts {@code "_padding":"xxx…",} after the opening brace so the UTF-8 body is exactly {@code targetBytes}. */
    static String paddedTo(String json, int targetBytes) {
        String trimmed = json.strip();
        assertThat(trimmed).startsWith("{");
        String prefix = "{\"_padding\":\"";
        String suffix = "\",";
        int overhead = (prefix + suffix).getBytes(StandardCharsets.UTF_8).length
                + trimmed.substring(1).getBytes(StandardCharsets.UTF_8).length;
        int padding = targetBytes - overhead;
        assertThat(padding)
                .as("fixture already larger than %d bytes", targetBytes)
                .isPositive();
        String padded = prefix + "x".repeat(padding) + suffix + trimmed.substring(1);
        assertThat(padded.getBytes(StandardCharsets.UTF_8)).hasSize(targetBytes);
        return padded;
    }
}
