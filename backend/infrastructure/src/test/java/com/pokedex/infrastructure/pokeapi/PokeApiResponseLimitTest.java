package com.pokedex.infrastructure.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.pokedex.domain.shared.PageRequest;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * The size cap wired into a real {@link RestClient} + HTTP interface proxy, end to end through the adapter:
 * an oversized PokeAPI response ends as {@link UpstreamUnavailableException} (503), never a partial page.
 */
class PokeApiResponseLimitTest {

    private static final int LIMIT = 1024;
    private static final String BASE = "https://pokeapi.test/api/v2";

    private MockRestServiceServer server;
    private PokeApiCatalogAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.requestInterceptor(new ResponseSizeLimitInterceptor(LIMIT))
                .build();
        PokeApiHttpClient client = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(PokeApiHttpClient.class);
        adapter = new PokeApiCatalogAdapter(new PokeApiGateway(client), new PokeApiMapper());
    }

    @Test
    void should_read_a_response_below_the_limit() {
        server.expect(requestTo(BASE + "/pokemon?offset=0&limit=3"))
                .andRespond(withSuccess(listJson(0), MediaType.APPLICATION_JSON));

        assertThat(adapter.fetchPage(new PageRequest(0, 3)).content()).isEmpty();
        server.verify();
    }

    @Test
    void should_fail_with_upstream_unavailable_when_content_length_exceeds_the_limit() {
        String body = listJson(LIMIT);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(body.length());
        server.expect(requestTo(BASE + "/pokemon?offset=0&limit=3"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON).headers(headers));

        assertThatThrownBy(() -> adapter.fetchPage(new PageRequest(0, 3)))
                .isInstanceOf(UpstreamUnavailableException.class)
                .hasRootCauseInstanceOf(ResponseTooLargeException.class);
    }

    @Test
    void should_fail_with_upstream_unavailable_when_the_streamed_body_exceeds_the_limit() {
        server.expect(requestTo(BASE + "/pokemon?offset=0&limit=3"))
                .andRespond(withSuccess(listJson(LIMIT), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.fetchPage(new PageRequest(0, 3)))
                .isInstanceOf(UpstreamUnavailableException.class)
                .hasStackTraceContaining("ResponseTooLargeException");
    }

    /** A valid, empty list page padded with an ignored field to the requested extra size. */
    private static String listJson(int padding) {
        return "{\"count\": 0, \"results\": [], \"pad\": \"" + "a".repeat(padding) + "\"}";
    }
}
