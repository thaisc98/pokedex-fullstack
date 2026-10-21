package com.pokedex.collection;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static org.assertj.core.api.Assertions.assertThat;

import com.pokedex.catalog.AbstractPokeApiIT;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Shared harness for the Collection acceptance tests (US03 sync, reads, US04 update, admin delete).
 *
 * <p>The collection is one shared team dataset (security.md §3.1) in one PostgreSQL container for the whole JVM,
 * keyed by {@code pokedexId} (BR-05). So every test syncs Pokémon with <b>its own</b> ids from
 * {@link #nextPokedexId()} (40000+, never a real PokeAPI id) and its own names and tags. PokeAPI has no such ids, so
 * the upstream answer is the <b>real</b> Pikachu fixtures with only {@code id}, {@code name}, {@code weight} and
 * the species/chain links changed in-test (see {@link #stubUpstream(int, String, int)}). Everything else (sprite,
 * abilities, stats, genus) is real Pikachu data.
 */
public abstract class AbstractCollectionIT extends AbstractPokeApiIT {

    public static final String COLLECTION_PATH = "/api/v1/collection";
    public static final String ITEM_PATH = COLLECTION_PATH + "/{pokedexId}";

    public static final String PIKACHU_SPRITE =
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png";

    private static final AtomicInteger NEXT_ID = new AtomicInteger(40_000);
    static final JsonMapper JSON = JsonMapper.builder().build();

    /** A pokedexId no other test uses in this JVM. */
    protected static int nextPokedexId() {
        return NEXT_ID.getAndIncrement();
    }

    /** A valid {@code PokemonName} unique to this test, e.g. {@code mon-40007-a}. */
    protected static String uniqueName(int pokedexId, String suffix) {
        return "mon-" + pokedexId + "-" + suffix;
    }

    /**
     * Stubs {@code /pokemon/{id}}, {@code /pokemon/{name}}, {@code /pokemon-species/{id}} and the evolution chain so
     * PokeAPI "has" Pokémon {@code id} called {@code name} weighing {@code weightHg}. Real Pikachu bodies, with
     * {@code id}, {@code name}, {@code weight}, {@code species.url} (→ species {@code id}) changed in-test.
     */
    protected static void stubUpstream(int id, String name, int weightHg) {
        stubUpstream(id, name, weightHg, 0);
    }

    /** As {@link #stubUpstream(int, String, int)}, with every upstream answer delayed by {@code delayMs}. */
    protected static void stubUpstream(int id, String name, int weightHg, int delayMs) {
        ObjectNode pokemon = (ObjectNode) JSON.readTree(fixture("pokemon/25.json"));
        pokemon.put("id", id);
        pokemon.put("name", name);
        pokemon.put("weight", weightHg);
        ((ObjectNode) pokemon.get("species")).put("name", name);
        ((ObjectNode) pokemon.get("species")).put("url", "https://pokeapi.co/api/v2/pokemon-species/" + id + "/");
        String pokemonJson = JSON.writeValueAsString(pokemon);
        stubPokemon(id, okJson(pokemonJson).withFixedDelay(delayMs));
        stubPokemonByName(name, okJson(pokemonJson).withFixedDelay(delayMs));

        ObjectNode species = (ObjectNode) JSON.readTree(fixture("pokemon-species/25.json"));
        species.put("id", id);
        species.put("name", name);
        stubSpecies(id, okJson(JSON.writeValueAsString(species)).withFixedDelay(delayMs));
        stubRecordedEvolutionChain(10); // the species still links to Pikachu's chain
    }

    // ------------------------------------------------------------ API calls

    protected static Response sync(Browser browser, int pokedexId) {
        return browser.write("POST", COLLECTION_PATH, Map.of("pokedexId", pokedexId));
    }

    /** Stubs upstream and syncs; asserts 201. Returns the created {@code CollectedPokemonDto}. */
    protected static Map<String, Object> syncNew(Browser browser, int pokedexId, String name) {
        stubUpstream(pokedexId, name, 60);
        Response created = sync(browser, pokedexId);
        assertThat(created.statusCode()).as("sync %d", pokedexId).isEqualTo(201);
        return created.jsonPath().getMap("$");
    }

    protected static Response get(Browser browser, int pokedexId) {
        return browser.get(COLLECTION_PATH + "/" + pokedexId);
    }

    protected static Response put(Browser browser, int pokedexId, Object body) {
        return browser.write("PUT", COLLECTION_PATH + "/" + pokedexId, body);
    }

    protected static Response delete(Browser browser, int pokedexId) {
        return browser.write("DELETE", COLLECTION_PATH + "/" + pokedexId, null);
    }

    protected static long version(Map<String, Object> dto) {
        return ((Number) dto.get("version")).longValue();
    }

    // ------------------------------------------------------------ request bodies

    /** {@code UpdateCollectedPokemonRequest}: full {@code customAttributes} + {@code version}. */
    protected static Map<String, Object> update(Map<String, Object> customAttributes, long version) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customAttributes", customAttributes);
        body.put("version", version);
        return body;
    }

    protected static Map<String, Object> custom(
            List<Map<String, Object>> localizedNames, @Nullable Map<String, Object> geo, List<String> tags) {
        Map<String, Object> custom = new LinkedHashMap<>();
        custom.put("localizedNames", localizedNames);
        custom.put("geo", geo);
        custom.put("tags", tags);
        return custom;
    }

    protected static Map<String, Object> customWithTags(String... tags) {
        return custom(List.of(), null, List.of(tags));
    }

    protected static Map<String, Object> name(String lang, String value) {
        return Map.of("lang", lang, "value", value);
    }

    protected static Map<String, Object> geo(String region, String habitat, double latitude, double longitude) {
        Map<String, Object> geo = new LinkedHashMap<>();
        geo.put("region", region);
        geo.put("habitat", habitat);
        geo.put("coordinates", Map.of("latitude", latitude, "longitude", longitude));
        return geo;
    }

    protected static List<String> tags(int count, String prefix) {
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tags.add(prefix + "-" + i);
        }
        return tags;
    }
}
