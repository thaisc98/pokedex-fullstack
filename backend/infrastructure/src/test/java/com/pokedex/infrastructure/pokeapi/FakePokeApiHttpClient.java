package com.pokedex.infrastructure.pokeapi;

import com.pokedex.infrastructure.pokeapi.dto.PokeApiListDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/** Map-backed {@link PokeApiHttpClient} that records every call. Thread-safe for the parallel fan-out. */
final class FakePokeApiHttpClient implements PokeApiHttpClient {

    private final Map<String, Supplier<PokeApiListDto>> lists = new ConcurrentHashMap<>();
    private final Map<Integer, Supplier<PokeApiPokemonDto>> pokemon = new ConcurrentHashMap<>();
    private final Map<Integer, Supplier<PokeApiSpeciesDto>> species = new ConcurrentHashMap<>();
    private final List<String> calls = new CopyOnWriteArrayList<>();

    FakePokeApiHttpClient list(long offset, int limit, Supplier<PokeApiListDto> response) {
        lists.put(offset + ":" + limit, response);
        return this;
    }

    FakePokeApiHttpClient pokemon(int id, Supplier<PokeApiPokemonDto> response) {
        pokemon.put(id, response);
        return this;
    }

    FakePokeApiHttpClient species(int id, Supplier<PokeApiSpeciesDto> response) {
        species.put(id, response);
        return this;
    }

    List<String> calls() {
        return List.copyOf(calls);
    }

    @Override
    public PokeApiListDto list(long offset, int limit) {
        calls.add("list?offset=" + offset + "&limit=" + limit);
        return answer(lists.get(offset + ":" + limit));
    }

    @Override
    public PokeApiPokemonDto pokemon(int id) {
        calls.add("pokemon/" + id);
        return answer(pokemon.get(id));
    }

    @Override
    public PokeApiSpeciesDto species(int id) {
        calls.add("pokemon-species/" + id);
        return answer(species.get(id));
    }

    private static <T> T answer(Supplier<T> response) {
        if (response == null) {
            throw new IllegalStateException("no stub");
        }
        return response.get();
    }
}
