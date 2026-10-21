package com.pokedex.infrastructure.pokeapi;

import com.pokedex.infrastructure.pokeapi.dto.PokeApiEvolutionChainDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiListDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Map-backed {@link PokeApiHttpClient} that records every call. Thread-safe for the parallel fan-out. An
 * unstubbed Pokémon, species or chain answers like PokeAPI: 404.
 */
final class FakePokeApiHttpClient implements PokeApiHttpClient {

    private final Map<String, Supplier<PokeApiListDto>> lists = new ConcurrentHashMap<>();
    private final Map<String, Supplier<PokeApiPokemonDto>> pokemon = new ConcurrentHashMap<>();
    private final Map<Integer, Supplier<PokeApiSpeciesDto>> species = new ConcurrentHashMap<>();
    private final Map<Integer, Supplier<PokeApiEvolutionChainDto>> chains = new ConcurrentHashMap<>();
    private final List<String> calls = new CopyOnWriteArrayList<>();

    FakePokeApiHttpClient list(long offset, int limit, Supplier<PokeApiListDto> response) {
        lists.put(offset + ":" + limit, response);
        return this;
    }

    FakePokeApiHttpClient pokemon(int id, Supplier<PokeApiPokemonDto> response) {
        pokemon.put(String.valueOf(id), response);
        return this;
    }

    FakePokeApiHttpClient pokemonNamed(String name, Supplier<PokeApiPokemonDto> response) {
        pokemon.put(name, response);
        return this;
    }

    FakePokeApiHttpClient species(int id, Supplier<PokeApiSpeciesDto> response) {
        species.put(id, response);
        return this;
    }

    FakePokeApiHttpClient evolutionChain(int id, Supplier<PokeApiEvolutionChainDto> response) {
        chains.put(id, response);
        return this;
    }

    List<String> calls() {
        return List.copyOf(calls);
    }

    @Override
    public PokeApiListDto list(long offset, int limit) {
        calls.add("list?offset=" + offset + "&limit=" + limit);
        Supplier<PokeApiListDto> response = lists.get(offset + ":" + limit);
        if (response == null) {
            throw new IllegalStateException("no stub");
        }
        return response.get();
    }

    @Override
    public PokeApiPokemonDto pokemon(int id) {
        calls.add("pokemon/" + id);
        return answer(pokemon.get(String.valueOf(id)));
    }

    @Override
    public PokeApiPokemonDto pokemonByName(String name) {
        calls.add("pokemon/" + name);
        return answer(pokemon.get(name));
    }

    @Override
    public PokeApiSpeciesDto species(int id) {
        calls.add("pokemon-species/" + id);
        return answer(species.get(id));
    }

    @Override
    public PokeApiEvolutionChainDto evolutionChain(int id) {
        calls.add("evolution-chain/" + id);
        return answer(chains.get(id));
    }

    static HttpClientErrorException notFound() {
        return HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
    }

    private static <T> T answer(Supplier<T> response) {
        if (response == null) {
            throw notFound();
        }
        return response.get();
    }
}
