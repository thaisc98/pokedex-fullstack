package com.pokedex.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;

import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Collection reads: {@code GET /api/v1/collection?page&size&tag&q} and {@code GET /api/v1/collection/{pokedexId}}
 * (UC-05, openapi {@code listCollection}, {@code getCollectedPokemon}).
 *
 * <p>Traceability: BR-01 paging rules, {@code TagFilterParam} (exact tag), {@code QueryParam} (case-insensitive name
 * search, {@code LIKE} wildcards escaped, security.md §4.4), security.md §3.2 (USER; anonymous 401).
 *
 * <p>The table is shared by every test in the JVM, so each test filters on a tag or name fragment that only its
 * own records carry.
 */
@DisplayName("Collection - list and get")
class ReadCollectionIT extends AbstractCollectionIT {

    @Test
    void should_page_through_records_filtered_by_tag() {
        Browser user = loggedInUser();
        String tag = "t-" + UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 3; i++) {
            int id = nextPokedexId();
            Map<String, Object> created = syncNew(user, id, uniqueName(id, "page"));
            put(user, id, update(customWithTags(tag), version(created))).then().statusCode(200);
        }

        Response first = user.get(COLLECTION_PATH + "?tag=" + tag + "&page=0&size=2");
        Response second = user.get(COLLECTION_PATH + "?tag=" + tag + "&page=1&size=2");

        first.then()
                .statusCode(200)
                .contentType(startsWith("application/json"))
                .body("page", equalTo(0))
                .body("size", equalTo(2))
                .body("totalElements", equalTo(3))
                .body("totalPages", equalTo(2));
        assertThat(first.jsonPath().getList("content")).hasSize(2);
        assertThat(second.jsonPath().getList("content")).hasSize(1);
        assertThat(first.jsonPath().getMap("$").keySet())
                .containsExactlyInAnyOrder("content", "page", "size", "totalElements", "totalPages");
        assertThat(first.jsonPath().getMap("content[0]").keySet())
                .containsExactlyInAnyOrder("id", "snapshot", "custom", "version", "syncedAt", "updatedAt");
        List<Integer> ids = new ArrayList<>(first.jsonPath().getList("content.id", Integer.class));
        ids.addAll(second.jsonPath().getList("content.id", Integer.class));
        assertThat(ids)
                .as("no record twice across pages")
                .doesNotHaveDuplicates()
                .hasSize(3);
        user.get(COLLECTION_PATH + "?tag=" + tag + "&page=5&size=2")
                .then()
                .statusCode(200)
                .body("content", empty()); // BR-01
    }

    @Test
    void should_filter_by_exact_tag_only() {
        Browser user = loggedInUser();
        String tag = "x-" + UUID.randomUUID().toString().substring(0, 8);
        int tagged = nextPokedexId();
        int prefixOnly = nextPokedexId();
        put(user, tagged, update(customWithTags(tag), version(syncNew(user, tagged, uniqueName(tagged, "a")))))
                .then()
                .statusCode(200);
        put(
                        user,
                        prefixOnly,
                        update(
                                customWithTags(tag + "-more"),
                                version(syncNew(user, prefixOnly, uniqueName(prefixOnly, "b")))))
                .then()
                .statusCode(200);

        user.get(COLLECTION_PATH + "?tag=" + tag)
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .body("content[0].id", equalTo(tagged));
    }

    @Test
    void should_search_by_name_case_insensitively() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        String fragment = "srch" + id;
        syncNew(user, id, "mon-" + fragment + "-pika");

        user.get(COLLECTION_PATH + "?q=" + fragment.toUpperCase())
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .body("content[0].snapshot.name", equalTo("mon-" + fragment + "-pika"));
    }

    @ParameterizedTest(name = "q={0} is literal, not a LIKE wildcard")
    @ValueSource(strings = {"%", "_", "%%", "mon-%", "mon_", "\\"})
    void should_treat_like_wildcards_in_q_literally(String q) {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        syncNew(user, id, uniqueName(id, "wild")); // names only contain [a-z0-9-], never % _ or \

        Response response = user.get(COLLECTION_PATH, Map.of("q", q));

        response.then().statusCode(200).body("totalElements", equalTo(0)).body("content", empty());
    }

    @ParameterizedTest(name = "{0}={1} -> 400")
    @CsvSource({"size,0", "size,101", "page,-1", "tag,Fire Type!", "q,''"})
    void should_reject_invalid_list_parameters_with_400(String param, String value) {
        loggedInUser()
                .get(COLLECTION_PATH, Map.of(param, value))
                .then()
                .statusCode(400)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/validation-error"))
                .body("errors.field", hasItem(param));
    }

    @Test
    void should_get_one_record_by_pokedex_id() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        Map<String, Object> created = syncNew(user, id, uniqueName(id, "one"));

        Response response = get(user, id);

        response.then().statusCode(200).body("id", equalTo(id));
        assertThat(response.jsonPath().getMap("$")).isEqualTo(created);
    }

    @Test
    void should_let_any_user_read_records_synced_by_another_user() {
        // Shared team dataset (security.md §3.1): no per-user records.
        Browser alice = loggedInUser();
        int id = nextPokedexId();
        syncNew(alice, id, uniqueName(id, "shared"));

        get(loggedInUser(), id).then().statusCode(200).body("id", equalTo(id));
    }

    @ParameterizedTest(name = "GET /collection/{0} -> 404")
    @ValueSource(strings = {"999999999"})
    void should_return_404_problem_when_record_is_missing(String pokedexId) {
        loggedInUser()
                .get(COLLECTION_PATH + "/" + pokedexId)
                .then()
                .statusCode(404)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/not-found"));
    }

    @ParameterizedTest(name = "{0} /collection/{1} -> 400 on pokedexId")
    @CsvSource({
        "GET, 0",
        "GET, -1",
        "GET, abc",
        "PUT, 0",
        "PUT, -1",
        "PUT, abc",
        "DELETE, 0",
        "DELETE, -1",
        "DELETE, abc"
    })
    void should_reject_invalid_pokedex_id_in_path_with_validation_problem(String method, String pokedexId) {
        // Lead decision: PokedexIdPathParam (minimum 1) violations are 400 validation-error on every item route.
        // DELETE runs as ADMIN so the role check cannot answer first.
        Browser browser = "DELETE".equals(method) ? loggedInAdmin() : loggedInUser();
        String path = COLLECTION_PATH + "/" + pokedexId;
        Response response = switch (method) {
            case "GET" -> browser.get(path);
            case "PUT" -> browser.write("PUT", path, update(customWithTags("x"), 0));
            default -> browser.write("DELETE", path, null);
        };

        response.then()
                .statusCode(400)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/validation-error"))
                .body("errors.field", hasItem("pokedexId"));
    }

    @ParameterizedTest(name = "anonymous GET {0} -> 401")
    @ValueSource(strings = {COLLECTION_PATH, COLLECTION_PATH + "/1"})
    void should_reject_anonymous_reads_with_401(String path) {
        new Browser().get(path).then().statusCode(401).body("type", equalTo("/problems/unauthorized"));
    }
}
