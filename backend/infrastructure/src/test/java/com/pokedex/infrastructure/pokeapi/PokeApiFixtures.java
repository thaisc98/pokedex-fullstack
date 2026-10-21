package com.pokedex.infrastructure.pokeapi;

import com.pokedex.infrastructure.pokeapi.dto.PokeApiEvolutionChainDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiListDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

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

    static PokeApiEvolutionChainDto evolutionChain(int id) {
        return read("evolution-chain/%d.json".formatted(id), PokeApiEvolutionChainDto.class);
    }

    /** A recorded fixture as an editable tree, for variants PokeAPI does not have. */
    static ObjectNode tree(String path) {
        try (InputStream in = PokeApiFixtures.class.getResourceAsStream("/pokeapi/" + path)) {
            if (in == null) {
                throw new IllegalStateException("Missing fixture " + path);
            }
            return (ObjectNode) JSON.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static <T> T convert(JsonNode node, Class<T> type) {
        return JSON.treeToValue(node, type);
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
