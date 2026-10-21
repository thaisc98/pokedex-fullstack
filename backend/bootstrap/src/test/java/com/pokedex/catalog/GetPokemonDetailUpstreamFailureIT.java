package com.pokedex.catalog;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * US02 failure modes: any of the three PokeAPI calls behind a detail ({@code /pokemon/{id}},
 * {@code /pokemon-species/{id}}, {@code /evolution-chain/{id}}) failing ends in 503 {@code ProblemDetail}
 * ({@code /problems/upstream-unavailable}), never a partial detail (architecture §1.5, openapi
 * {@code UpstreamUnavailable}, testing-strategy §5, security.md §4.5 1 MB cap and §4.6 no leaks).
 *
 * <p>All other calls answer with the real recorded Pikachu fixtures, so only the named call fails.
 */
@DisplayName("US02 - Pokémon detail when PokeAPI fails")
class GetPokemonDetailUpstreamFailureIT extends AbstractPokeApiIT {

    @ParameterizedTest(name = "{0} call answers {1} -> 503")
    @CsvSource({
        "pokemon,         HTTP_500",
        "pokemon,         HTTP_503",
        "pokemon,         CONNECTION_RESET",
        "pokemon,         OVER_1_MB",
        "species,         HTTP_500",
        "species,         HTTP_503",
        "species,         CONNECTION_RESET",
        "species,         OVER_1_MB",
        "evolution-chain, HTTP_500",
        "evolution-chain, HTTP_503",
        "evolution-chain, CONNECTION_RESET",
        "evolution-chain, OVER_1_MB",
    })
    void should_return_503_without_partial_detail_when_one_upstream_call_fails(String call, String failure) {
        stubPokemon(25, "pokemon".equals(call) ? failing(failure, "pokemon/25.json") : recorded("pokemon/25.json"));
        stubSpecies(
                25,
                "species".equals(call)
                        ? failing(failure, "pokemon-species/25.json")
                        : recorded("pokemon-species/25.json"));
        stubEvolutionChain(
                10,
                "evolution-chain".equals(call)
                        ? failing(failure, "evolution-chain/10.json")
                        : recorded("evolution-chain/10.json"));

        assertUpstreamUnavailableProblem(GetPokemonDetailIT.requestDetail("25"));
    }

    private static ResponseDefinitionBuilder recorded(String fixturePath) {
        return okJson(fixture(fixturePath));
    }

    private static ResponseDefinitionBuilder failing(String failure, String fixturePath) {
        return switch (failure) {
            case "HTTP_500" -> status(500);
            case "HTTP_503" -> status(503);
            case "CONNECTION_RESET" -> aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER);
            // Real fixture padded to 1.2 MB: valid JSON, only the size is wrong (security.md §4.5).
            case "OVER_1_MB" ->
                okJson(PokeApiResponseSizeLimitIT.paddedTo(
                        fixture(fixturePath), PokeApiResponseSizeLimitIT.OVER_LIMIT_BYTES));
            default -> throw new IllegalArgumentException(failure);
        };
    }
}
