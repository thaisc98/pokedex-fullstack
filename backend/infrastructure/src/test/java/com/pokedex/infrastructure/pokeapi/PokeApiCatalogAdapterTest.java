package com.pokedex.infrastructure.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiListDto;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/** List → per-Pokémon detail + species fan-out; any failure fails the whole page (architecture.md §1.5). */
class PokeApiCatalogAdapterTest {

    private final FakePokeApiHttpClient client = new FakePokeApiHttpClient();
    private final PokeApiCatalogAdapter adapter =
            new PokeApiCatalogAdapter(new PokeApiGateway(client), new PokeApiMapper());

    @Test
    void should_build_the_page_in_upstream_order_with_the_upstream_count() {
        stubFirstThree();

        Page<PokemonSummary> page = adapter.fetchPage(new PageRequest(0, 3));

        assertThat(page.content())
                .extracting(s -> s.name().value())
                .containsExactly("bulbasaur", "ivysaur", "venusaur");
        assertThat(page.content()).extracting(s -> s.mass().kilograms()).containsExactly(6.9, 13.0, 100.0);
        assertThat(page.totalElements()).isEqualTo(1351);
        assertThat(page.request()).isEqualTo(new PageRequest(0, 3));
    }

    @Test
    void should_ask_pokeapi_for_the_offset_and_limit_of_the_request() {
        stubFirstThree();

        adapter.fetchPage(new PageRequest(0, 3));

        assertThat(client.calls()).first().isEqualTo("list?offset=0&limit=3");
        assertThat(client.calls())
                .contains(
                        "pokemon/1",
                        "pokemon/2",
                        "pokemon/3",
                        "pokemon-species/1",
                        "pokemon-species/2",
                        "pokemon-species/3");
    }

    @Test
    void should_pass_a_huge_offset_as_a_long_without_overflow() {
        client.list(214_748_364_700L, 100, () -> PokeApiFixtures.list(2_000_000, 20));

        Page<PokemonSummary> page = adapter.fetchPage(new PageRequest(Integer.MAX_VALUE, 100));

        assertThat(page.content()).isEmpty();
        assertThat(client.calls()).containsExactly("list?offset=214748364700&limit=100");
    }

    @Test
    void should_fetch_the_species_by_the_id_in_the_species_url_not_the_pokemon_id() {
        client.list(1288, 1, () -> PokeApiFixtures.list(1288, 1))
                .pokemon(10264, () -> PokeApiFixtures.pokemon(10264))
                .species(1007, () -> PokeApiFixtures.species(1007));

        Page<PokemonSummary> page = adapter.fetchPage(new PageRequest(1288, 1));

        assertThat(page.content())
                .singleElement()
                .satisfies(s -> assertThat(s.id().value()).isEqualTo(10264));
        assertThat(client.calls()).containsExactly("list?offset=1288&limit=1", "pokemon/10264", "pokemon-species/1007");
    }

    @Test
    void should_return_an_empty_page_past_the_end_without_item_calls() {
        client.list(2_000_000, 20, () -> PokeApiFixtures.list(2_000_000, 20));

        Page<PokemonSummary> page = adapter.fetchPage(new PageRequest(100_000, 20));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(1351);
        assertThat(client.calls()).hasSize(1);
    }

    @Test
    void should_fail_the_whole_page_when_one_pokemon_call_fails() {
        stubFirstThree();
        client.pokemon(2, () -> {
            throw serverError();
        });

        assertThatThrownBy(() -> adapter.fetchPage(new PageRequest(0, 3)))
                .isInstanceOf(UpstreamUnavailableException.class)
                .hasRootCauseInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void should_fail_the_whole_page_when_one_species_call_fails() {
        stubFirstThree();
        client.species(3, () -> {
            throw new ResourceAccessException("connection reset");
        });

        assertThatThrownBy(() -> adapter.fetchPage(new PageRequest(0, 3)))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void should_fail_when_the_list_call_fails_with_a_client_error() {
        client.list(0, 3, () -> {
            throw HttpClientErrorException.create(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too Many Requests",
                    HttpHeaders.EMPTY,
                    new byte[0],
                    StandardCharsets.UTF_8);
        });

        assertThatThrownBy(() -> adapter.fetchPage(new PageRequest(0, 3)))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void should_fail_when_the_upstream_body_cannot_be_read() {
        client.list(0, 3, () -> {
            throw new RestClientException("malformed JSON");
        });

        assertThatThrownBy(() -> adapter.fetchPage(new PageRequest(0, 3)))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void should_fail_when_upstream_data_breaks_the_domain_rules() {
        stubFirstThree();
        client.pokemon(
                1,
                () -> PokeApiFixtures.parse(
                        "{\"id\": 1, \"name\": \"Bulbasaur!\", \"weight\": 69, \"abilities\": [],"
                                + " \"species\": {\"name\": \"b\", \"url\": \"https://pokeapi.co/api/v2/pokemon-species/1/\"}}",
                        com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto.class));

        assertThatThrownBy(() -> adapter.fetchPage(new PageRequest(0, 3)))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void should_fail_when_a_list_result_url_does_not_match_the_strict_pattern() {
        client.list(
                0,
                1,
                () -> PokeApiFixtures.parse(
                        "{\"count\": 1, \"results\": [{\"name\": \"x\", \"url\": \"https://evil.example/steal\"}]}",
                        PokeApiListDto.class));

        assertThatThrownBy(() -> adapter.fetchPage(new PageRequest(0, 1)))
                .isInstanceOf(UpstreamUnavailableException.class);
        assertThat(client.calls()).containsExactly("list?offset=0&limit=1");
    }

    @Test
    void should_fail_when_upstream_returns_more_results_than_requested() {
        client.list(0, 2, () -> PokeApiFixtures.list(0, 3));

        assertThatThrownBy(() -> adapter.fetchPage(new PageRequest(0, 2)))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void should_fail_when_the_list_body_has_no_results() {
        client.list(0, 2, () -> PokeApiFixtures.parse("{\"count\": 5}", PokeApiListDto.class));

        assertThatThrownBy(() -> adapter.fetchPage(new PageRequest(0, 2)))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void should_not_leak_upstream_details_in_the_exception_message() {
        client.list(0, 3, () -> {
            throw serverError();
        });

        assertThatThrownBy(() -> adapter.fetchPage(new PageRequest(0, 3)))
                .isInstanceOf(UpstreamUnavailableException.class)
                .hasMessage("PokeAPI is unavailable");
    }

    private void stubFirstThree() {
        client.list(0, 3, () -> PokeApiFixtures.list(0, 3));
        for (int id = 1; id <= 3; id++) {
            int current = id;
            client.pokemon(current, () -> PokeApiFixtures.pokemon(current));
            client.species(current, () -> PokeApiFixtures.species(current));
        }
    }

    private static HttpServerErrorException serverError() {
        return HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Server Error",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8);
    }
}
