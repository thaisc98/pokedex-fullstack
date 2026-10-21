package com.pokedex.collection;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import com.github.tomakehurst.wiremock.http.Fault;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * US03 sync: {@code POST /api/v1/collection {pokedexId}} (UC-03, openapi {@code syncCollection}).
 *
 * <p>Traceability: BR-05 (idempotent upsert keyed by {@code PokedexId}; first time 201 + {@code Location}, re-sync
 * 200 refreshing {@code UpstreamSnapshot} and never touching {@code CustomAttributes}); testing-strategy.md §5 Sync
 * (unknown upstream → 404 and no row; upstream down → 503 and no row; two syncs at once → one row, never 500;
 * re-sync updates {@code syncedAt}); security.md §3.2 (USER), §4.2.1 (CSRF), §4.3 (unknown fields, 16 KB, 415).
 */
@DisplayName("US03 - sync a Pokémon into the collection")
class SyncCollectionIT extends AbstractCollectionIT {

    @Test
    void should_create_collected_pokemon_with_201_location_and_upstream_snapshot() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        String name = uniqueName(id, "new");
        stubUpstream(id, name, 60);

        Response response = sync(user, id);

        response.then()
                .statusCode(201)
                .contentType(startsWith("application/json"))
                .header("Location", endsWith("/api/v1/collection/" + id))
                .body("id", equalTo(id))
                .body("snapshot.id", equalTo(id))
                .body("snapshot.name", equalTo(name))
                .body("snapshot.sprite", equalTo(PIKACHU_SPRITE))
                .body("snapshot.category", equalTo("Mouse Pokémon"))
                .body("snapshot.abilities.name", contains("static", "lightning-rod"))
                .body("snapshot.abilities.hidden", contains(false, true))
                .body(
                        "snapshot.stats.name",
                        contains("hp", "attack", "defense", "special-attack", "special-defense", "speed"))
                .body("snapshot.stats.value", contains(35, 55, 40, 50, 50, 90))
                .body("custom.localizedNames", empty())
                .body("custom.tags", empty());
        JsonPath json = response.jsonPath();
        assertThat(json.getMap("$").keySet())
                .as("CollectedPokemonDto fields exactly")
                .containsExactlyInAnyOrder("id", "snapshot", "custom", "version", "syncedAt", "updatedAt");
        assertThat(json.getMap("snapshot").keySet())
                .containsExactlyInAnyOrder("id", "name", "sprite", "category", "mass", "abilities", "stats");
        assertThat(new BigDecimal(json.getString("snapshot.mass"))).isEqualByComparingTo("6.0");
        assertThat(json.getLong("version")).isGreaterThanOrEqualTo(0);
        assertThat(json.get("custom.geo") == null).as("no geo yet").isTrue();
        Instant.parse(json.getString("syncedAt"));
        Instant.parse(json.getString("updatedAt"));

        get(user, id).then().statusCode(200).body("snapshot.name", equalTo(name));
    }

    @Test
    void should_refresh_snapshot_with_200_and_keep_custom_attributes_when_resyncing() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        String name = uniqueName(id, "resync");
        Map<String, Object> created = syncNew(user, id, name);
        Map<String, Object> custom =
                custom(List.of(name("ja", "ピカチュウ")), geo("Kanto", "forest", 35.6, 139.7), List.of("starter"));
        Response updated = put(user, id, update(custom, version(created)));
        updated.then().statusCode(200);
        Instant syncedBefore = Instant.parse(updated.jsonPath().getString("syncedAt"));

        // PokeAPI changed in the meantime: heavier now. The catalog cache is cleared to stand in for its TTL
        // expiring, so the re-sync really sees the new upstream data.
        stubUpstream(id, name, 75);
        clearAllCaches();
        Response resynced = sync(user, id);

        resynced.then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("custom.localizedNames[0].lang", equalTo("ja"))
                .body("custom.localizedNames[0].value", equalTo("ピカチュウ"))
                .body("custom.geo.region", equalTo("Kanto"))
                .body("custom.geo.habitat", equalTo("forest"))
                .body("custom.tags", contains("starter"));
        assertThat(resynced.getHeader("Location"))
                .as("no Location on a refresh")
                .isNull();
        assertThat(new BigDecimal(resynced.jsonPath().getString("snapshot.mass")))
                .as("snapshot refreshed from upstream (BR-05)")
                .isEqualByComparingTo("7.5");
        assertThat(Instant.parse(resynced.jsonPath().getString("syncedAt"))).isAfter(syncedBefore);
        assertThat(resynced.jsonPath().getMap("custom"))
                .as("custom attributes untouched by the re-sync")
                .isEqualTo(updated.jsonPath().getMap("custom"));
    }

    @Test
    void should_keep_a_single_row_when_the_same_pokemon_is_synced_twice_at_once() {
        Browser first = loggedInUser();
        Browser second = loggedInUser();
        first.fetchCsrf();
        second.fetchCsrf();
        int id = nextPokedexId();
        String name = uniqueName(id, "race");
        // A slow upstream widens the window in which both requests are in flight (no sleep in the test itself).
        stubUpstream(id, name, 60, 300);

        CompletableFuture<Response> a = CompletableFuture.supplyAsync(() -> sync(first, id));
        CompletableFuture<Response> b = CompletableFuture.supplyAsync(() -> sync(second, id));
        List<Integer> statuses = List.of(a.join().statusCode(), b.join().statusCode());

        assertThat(statuses)
                .as("one create, one refresh; the unique-constraint race is never a 500")
                .containsExactlyInAnyOrder(201, 200);
        Response listed = first.get(COLLECTION_PATH + "?q=" + name);
        listed.then().statusCode(200).body("totalElements", equalTo(1));
    }

    @Test
    void should_return_404_and_write_nothing_when_pokemon_is_unknown_upstream() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        stubPokemon(id, pokeApiNotFound());

        sync(user, id)
                .then()
                .statusCode(404)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/not-found"));
        get(user, id).then().statusCode(404);
    }

    @Test
    void should_return_503_and_write_nothing_when_pokeapi_is_down() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        stubUpstream(id, uniqueName(id, "down"), 60);
        stubSpecies(id, aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)); // fails mid-sync

        assertUpstreamUnavailableProblem(sync(user, id));
        get(user, id).then().statusCode(404);
    }

    @ParameterizedTest(name = "body {0} -> 400")
    @ValueSource(
            strings = {
                "{}",
                "{\"pokedexId\":0}",
                "{\"pokedexId\":-1}",
                "{\"pokedexId\":\"abc\"}",
                "{\"pokedexId\":25,\"name\":\"hacked\"}",
                "not json"
            })
    void should_reject_invalid_sync_body_with_400(String body) {
        loggedInUser()
                .write("POST", COLLECTION_PATH, body)
                .then()
                .statusCode(400)
                .contentType(startsWith(PROBLEM_JSON));
    }

    @Test
    void should_reject_sync_with_wrong_content_type_with_415() {
        Browser user = loggedInUser();

        user.writeRaw("POST", COLLECTION_PATH, "{\"pokedexId\":25}", "text/plain")
                .then()
                .statusCode(415)
                .contentType(startsWith(PROBLEM_JSON));
    }

    @Test
    void should_reject_sync_without_csrf_token() {
        Browser user = loggedInUser();

        user.writeWithCsrfHeader("POST", COLLECTION_PATH, "{\"pokedexId\":25}", null)
                .then()
                .statusCode(403)
                .body("type", equalTo("/problems/csrf-token-invalid"));
    }

    @Test
    void should_reject_anonymous_sync_with_401() {
        new Browser()
                .write("POST", COLLECTION_PATH, Map.of("pokedexId", nextPokedexId()))
                .then()
                .statusCode(401)
                .body("type", equalTo("/problems/unauthorized"));
    }
}
