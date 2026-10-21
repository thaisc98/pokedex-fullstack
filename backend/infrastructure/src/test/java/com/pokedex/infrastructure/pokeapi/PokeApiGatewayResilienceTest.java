package com.pokedex.infrastructure.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.infrastructure.pokeapi.dto.PokeApiListDto;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * Framework 7 {@code @Retryable} on {@link PokeApiGateway}: retried only on 5xx and IO errors, never on 4xx
 * (429 included) or unreadable bodies (backend/AGENTS.md "HTTP clients"). Runs in a plain Spring context with
 * {@code @EnableResilientMethods}, so the real proxy is exercised. The delay is shrunk through the property.
 */
class PokeApiGatewayResilienceTest {

    private AnnotationConfigApplicationContext context;

    @AfterEach
    void close() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void should_retry_three_times_on_a_5xx_response_then_give_up() {
        AtomicInteger attempts = new AtomicInteger();
        PokeApiGateway gateway = gatewayAnswering(() -> {
            attempts.incrementAndGet();
            throw HttpServerErrorException.create(
                    HttpStatus.BAD_GATEWAY, "Bad Gateway", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        });

        assertThatThrownBy(() -> gateway.list(0, 3)).isInstanceOf(HttpServerErrorException.class);
        assertThat(attempts).hasValue(4);
    }

    @Test
    void should_retry_on_an_io_error() {
        AtomicInteger attempts = new AtomicInteger();
        PokeApiGateway gateway = gatewayAnswering(() -> {
            attempts.incrementAndGet();
            throw new ResourceAccessException("connection reset");
        });

        assertThatThrownBy(() -> gateway.list(0, 3)).isInstanceOf(ResourceAccessException.class);
        assertThat(attempts).hasValue(4);
    }

    @Test
    void should_succeed_when_a_retry_succeeds() {
        AtomicInteger attempts = new AtomicInteger();
        PokeApiListDto ok = new PokeApiListDto(0L, List.of());
        PokeApiGateway gateway = gatewayAnswering(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw new ResourceAccessException("timeout");
            }
            return ok;
        });

        assertThat(gateway.list(0, 3)).isSameAs(ok);
        assertThat(attempts).hasValue(2);
    }

    @Test
    void should_not_retry_on_429() {
        AtomicInteger attempts = new AtomicInteger();
        PokeApiGateway gateway = gatewayAnswering(() -> {
            attempts.incrementAndGet();
            throw HttpClientErrorException.create(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too Many Requests",
                    HttpHeaders.EMPTY,
                    new byte[0],
                    StandardCharsets.UTF_8);
        });

        assertThatThrownBy(() -> gateway.list(0, 3)).isInstanceOf(HttpClientErrorException.class);
        assertThat(attempts).hasValue(1);
    }

    @Test
    void should_not_retry_on_404() {
        AtomicInteger attempts = new AtomicInteger();
        PokeApiGateway gateway = gatewayAnswering(() -> {
            attempts.incrementAndGet();
            throw HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        });

        assertThatThrownBy(() -> gateway.list(0, 3)).isInstanceOf(HttpClientErrorException.class);
        assertThat(attempts).hasValue(1);
    }

    @Test
    void should_not_retry_an_oversized_response_even_when_reported_as_an_io_error() {
        // RestClient wraps an IOException from an interceptor (Content-Length over the cap) in
        // ResourceAccessException, which is otherwise retryable. Retrying cannot shrink the body.
        AtomicInteger attempts = new AtomicInteger();
        PokeApiGateway gateway = gatewayAnswering(() -> {
            attempts.incrementAndGet();
            throw new ResourceAccessException("I/O error", new ResponseTooLargeException());
        });

        assertThatThrownBy(() -> gateway.list(0, 3)).isInstanceOf(ResourceAccessException.class);
        assertThat(attempts).hasValue(1);
    }

    @Test
    void should_not_retry_an_oversized_streamed_body() {
        AtomicInteger attempts = new AtomicInteger();
        PokeApiGateway gateway = gatewayAnswering(() -> {
            attempts.incrementAndGet();
            throw new RestClientException("Error while extracting response", new ResponseTooLargeException());
        });

        assertThatThrownBy(() -> gateway.list(0, 3)).isInstanceOf(RestClientException.class);
        assertThat(attempts).hasValue(1);
    }

    @Test
    void should_not_retry_on_an_unreadable_body() {
        AtomicInteger attempts = new AtomicInteger();
        PokeApiGateway gateway = gatewayAnswering(() -> {
            attempts.incrementAndGet();
            throw new RestClientException("malformed JSON");
        });

        assertThatThrownBy(() -> gateway.list(0, 3)).isInstanceOf(RestClientException.class);
        assertThat(attempts).hasValue(1);
    }

    @Test
    void should_retry_the_pokemon_and_species_calls_too() {
        AtomicInteger attempts = new AtomicInteger();
        FakePokeApiHttpClient client = new FakePokeApiHttpClient()
                .pokemon(1, () -> {
                    attempts.incrementAndGet();
                    throw new ResourceAccessException("reset");
                })
                .species(1, () -> {
                    attempts.incrementAndGet();
                    throw new ResourceAccessException("reset");
                });
        PokeApiGateway gateway = proxied(client);

        assertThatThrownBy(() -> gateway.pokemon(1)).isInstanceOf(ResourceAccessException.class);
        assertThatThrownBy(() -> gateway.species(1)).isInstanceOf(ResourceAccessException.class);
        assertThat(attempts).hasValue(8);
    }

    private PokeApiGateway gatewayAnswering(Supplier<PokeApiListDto> list) {
        return proxied(new FakePokeApiHttpClient().list(0, 3, list));
    }

    private PokeApiGateway proxied(PokeApiHttpClient client) {
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment()
                .getPropertySources()
                .addFirst(new MapPropertySource("test", Map.of("pokeapi.retry.delay", "1ms")));
        context.register(ResilienceConfig.class);
        context.registerBean(PokeApiGateway.class, () -> new PokeApiGateway(client));
        context.refresh();
        return context.getBean(PokeApiGateway.class);
    }

    @Test
    void should_retry_the_name_lookup_and_evolution_chain_calls_but_not_a_404() {
        AtomicInteger attempts = new AtomicInteger();
        FakePokeApiHttpClient client = new FakePokeApiHttpClient()
                .pokemonNamed("pikachu", () -> {
                    attempts.incrementAndGet();
                    throw new ResourceAccessException("reset");
                })
                .evolutionChain(10, () -> {
                    attempts.incrementAndGet();
                    throw new ResourceAccessException("reset");
                });
        PokeApiGateway gateway = proxied(client);

        assertThatThrownBy(() -> gateway.pokemonByName("pikachu")).isInstanceOf(ResourceAccessException.class);
        assertThatThrownBy(() -> gateway.evolutionChain(10)).isInstanceOf(ResourceAccessException.class);
        assertThat(attempts).hasValue(8);

        // unstubbed -> PokeAPI-style 404: a client error, never retried
        assertThatThrownBy(() -> gateway.pokemon(99999)).isInstanceOf(HttpClientErrorException.NotFound.class);
        assertThat(client.calls()).filteredOn("pokemon/99999"::equals).hasSize(1);
    }

    // Class-based proxy, as in bootstrap: the gateway also implements PokeApiResources.
    @EnableResilientMethods(proxyTargetClass = true)
    static class ResilienceConfig {}
}
