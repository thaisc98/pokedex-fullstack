package com.pokedex.infrastructure.pokeapi;

import com.pokedex.infrastructure.pokeapi.dto.PokeApiListDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;
import java.util.Objects;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Resilience boundary around {@link PokeApiHttpClient} (Framework 7, enabled by {@code
 * @EnableResilientMethods} in bootstrap):
 *
 * <ul>
 *   <li>{@code @Retryable}: only 5xx ({@link HttpServerErrorException}) and IO errors ({@link
 *       ResourceAccessException}: connection refused/reset, timeouts). 4xx, 429 included, and unreadable bodies
 *       are not retried. Up to 3 retries with exponential backoff (200 ms, 400 ms, 800 ms by default).
 *   <li>{@code @ConcurrencyLimit} at type level: every PokeAPI call shares one throttle of 10.
 * </ul>
 *
 * <p>Not final and with public methods so Spring can proxy it.
 */
@Retryable(
        includes = {HttpServerErrorException.class, ResourceAccessException.class},
        // An oversized body (security.md §4.5) arrives as an IO error but retrying cannot shrink it.
        excludes = ResponseTooLargeException.class,
        maxRetriesString = "${pokeapi.retry.max-retries:3}",
        delayString = "${pokeapi.retry.delay:200ms}",
        multiplier = 2.0)
@ConcurrencyLimit(limitString = "${pokeapi.max-concurrency:10}")
public class PokeApiGateway {

    private final PokeApiHttpClient client;

    public PokeApiGateway(PokeApiHttpClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public PokeApiListDto list(long offset, int limit) {
        return client.list(offset, limit);
    }

    public PokeApiPokemonDto pokemon(int id) {
        return client.pokemon(id);
    }

    public PokeApiSpeciesDto species(int id) {
        return client.species(id);
    }
}
