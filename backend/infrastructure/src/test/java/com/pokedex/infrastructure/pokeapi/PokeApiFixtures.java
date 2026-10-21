package com.pokedex.infrastructure.pokeapi;

import com.pokedex.infrastructure.pokeapi.dto.PokeApiListDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the recorded PokeAPI responses under {@code src/test/resources/pokeapi/} (owned by the test engineer)
 * into the client DTOs with a default Jackson 3 mapper, the same way RestClient will.
 */
final class PokeApiFixtures {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private PokeApiFixtures() {}

    static PokeApiListDto list(int offset, int limit) {
        return read("pokemon-list/offset-%d-limit-%d.json".formatted(offset, limit), PokeApiListDto.class);
    }

    static PokeApiPokemonDto pokemon(int id) {
        return read("pokemon/%d.json".formatted(id), PokeApiPokemonDto.class);
    }

    static PokeApiSpeciesDto species(int id) {
        return read("pokemon-species/%d.json".formatted(id), PokeApiSpeciesDto.class);
    }

    static <T> T parse(String json, Class<T> type) {
        return JSON.readValue(json, type);
    }

    private static <T> T read(String path, Class<T> type) {
        try (InputStream in = PokeApiFixtures.class.getResourceAsStream("/pokeapi/" + path)) {
            if (in == null) {
                throw new IllegalStateException("Missing fixture " + path);
            }
            return JSON.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
