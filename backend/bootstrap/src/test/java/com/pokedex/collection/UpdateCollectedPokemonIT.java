package com.pokedex.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import io.restassured.response.Response;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * US04 update: {@code PUT /api/v1/collection/{pokedexId}} (UC-04, openapi {@code updateCollectedPokemon}).
 *
 * <p>Traceability: BR-06 (only {@code customAttributes} change; upstream fields in the body → 400), BR-07 (tags
 * trimmed, lowercased, de-duplicated, at most 10), BR-08 (stale {@code version} → 409
 * {@code /problems/version-conflict}); architecture §1.2 ({@code LocalizedName} BCP-47 + 1–60 chars, one per
 * language; {@code GeoMetadata} region/habitat ≤ 60, lat −90..90, lon −180..180); security.md §4.3 (unknown fields
 * → 400, 16 KB → 413, non-JSON → 415, blank strings → 400 after trimming), §4.4 (injection text stored as plain
 * text), §4.2.1 (CSRF); testing-strategy.md §5 Update.
 */
@DisplayName("US04 - update custom attributes")
class UpdateCollectedPokemonIT extends AbstractCollectionIT {

    @Test
    void should_replace_custom_attributes_and_increment_version() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        Map<String, Object> created = syncNew(user, id, uniqueName(id, "upd"));
        Map<String, Object> custom = custom(
                List.of(name("ja", "ピカチュウ"), name("pt-BR", "Pikachu brasileiro")),
                geo("Kanto", "forest", 35.6895, 139.6917),
                List.of("starter", "electric"));

        Response response = put(user, id, update(custom, version(created)));

        response.then()
                .statusCode(200)
                .contentType(startsWith("application/json"))
                .body("id", equalTo(id))
                .body("version", equalTo((int) version(created) + 1))
                .body("custom.geo.region", equalTo("Kanto"))
                .body("custom.geo.habitat", equalTo("forest"));
        Map<String, Object> body = response.jsonPath().getMap("$");
        assertThat(response.jsonPath().getList("custom.tags", String.class))
                .containsExactlyInAnyOrder("starter", "electric");
        assertThat(response.jsonPath().getList("custom.localizedNames"))
                .containsExactlyInAnyOrder(name("ja", "ピカチュウ"), name("pt-BR", "Pikachu brasileiro"));
        assertThat(response.jsonPath().getDouble("custom.geo.coordinates.latitude"))
                .isEqualTo(35.6895);
        assertThat(response.jsonPath().getDouble("custom.geo.coordinates.longitude"))
                .isEqualTo(139.6917);
        assertThat(body.get("snapshot")).as("BR-06: snapshot untouched").isEqualTo(created.get("snapshot"));
        assertThat(Instant.parse((String) body.get("updatedAt")))
                .isAfter(Instant.parse((String) created.get("updatedAt")));

        // Persisted, not just echoed.
        assertThat(get(user, id).jsonPath().getMap("$")).isEqualTo(body);
    }

    @Test
    void should_normalize_tags_trimmed_lowercased_and_deduplicated() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        Map<String, Object> created = syncNew(user, id, uniqueName(id, "tags"));

        Response response = put(
                user, id, update(customWithTags("  Fire-Type ", "fire-type", "STARTER", "starter"), version(created)));

        response.then().statusCode(200);
        assertThat(response.jsonPath().getList("custom.tags", String.class))
                .containsExactlyInAnyOrder("fire-type", "starter");
    }

    @Test
    void should_store_injection_text_as_plain_text() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        Map<String, Object> created = syncNew(user, id, uniqueName(id, "inject"));
        String sql = "x'); DROP TABLE app_user;--";
        String script = "<script>alert(1)</script>";

        Response response = put(
                user,
                id,
                update(
                        custom(List.of(name("en", script)), geoWithoutCoordinates(sql, "cave"), List.of()),
                        version(created)));

        response.then()
                .statusCode(200)
                .body("custom.geo.region", equalTo(sql))
                .body("custom.localizedNames[0].value", equalTo(script));
        // app_user still exists: the SQL text was bound as a parameter, never executed.
        user.get(Browser.ME_PATH).then().statusCode(200);
    }

    @Test
    void should_return_409_version_conflict_when_version_is_stale() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        Map<String, Object> created = syncNew(user, id, uniqueName(id, "stale"));
        long seen = version(created);
        put(user, id, update(customWithTags("first"), seen)).then().statusCode(200);

        put(user, id, update(customWithTags("second"), seen))
                .then()
                .statusCode(409)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/version-conflict"));
        assertThat(get(user, id).jsonPath().getList("custom.tags", String.class))
                .as("the stale write changed nothing")
                .containsExactly("first");
    }

    @Test
    void should_return_404_when_record_does_not_exist() {
        put(loggedInUser(), nextPokedexId(), update(customWithTags("ghost"), 0))
                .then()
                .statusCode(404)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/not-found"));
    }

    static Stream<Arguments> invalidCustomAttributes() {
        return Stream.of(
                Arguments.of("11 tags (max 10, BR-07)", customWithTagsList(tags(11, "t"))),
                Arguments.of("tag with space and !", customWithTags("Fire Type!")),
                Arguments.of("tag of 31 chars", customWithTags("a".repeat(31))),
                Arguments.of(
                        "two names for one language",
                        custom(List.of(name("ja", "A"), name("ja", "B")), null, List.of())),
                Arguments.of("invalid language tag", custom(List.of(name("not a language", "X")), null, List.of())),
                Arguments.of(
                        "localized value of 61 chars", custom(List.of(name("en", "x".repeat(61))), null, List.of())),
                Arguments.of("blank localized value", custom(List.of(name("en", "   ")), null, List.of())),
                Arguments.of("latitude 91", custom(List.of(), geo("Kanto", "forest", 91, 0), List.of())),
                Arguments.of("latitude -90.0001", custom(List.of(), geo("Kanto", "forest", -90.0001, 0), List.of())),
                Arguments.of("longitude -181", custom(List.of(), geo("Kanto", "forest", 0, -181), List.of())),
                Arguments.of("longitude 180.5", custom(List.of(), geo("Kanto", "forest", 0, 180.5), List.of())),
                Arguments.of(
                        "region of 61 chars",
                        custom(List.of(), geoWithoutCoordinates("r".repeat(61), "forest"), List.of())),
                Arguments.of("blank region", custom(List.of(), geoWithoutCoordinates("   ", "forest"), List.of())));
    }

    @ParameterizedTest(name = "{0} -> 400")
    @MethodSource("invalidCustomAttributes")
    void should_reject_invalid_custom_attributes_with_validation_problem(
            String reason, Map<String, Object> customAttributes) {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        Map<String, Object> created = syncNew(user, id, uniqueName(id, "bad"));

        put(user, id, update(customAttributes, version(created)))
                .then()
                .statusCode(400)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/validation-error"));
        assertThat(version(get(user, id).jsonPath().getMap("$")))
                .as("nothing written")
                .isEqualTo(version(created));
    }

    @Test
    void should_accept_ten_tags_and_boundary_coordinates() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        Map<String, Object> created = syncNew(user, id, uniqueName(id, "edge"));

        put(user, id, update(custom(List.of(), geo("Kanto", "forest", -90, 180), tags(10, "t")), version(created)))
                .then()
                .statusCode(200);
    }

    static Stream<Arguments> forbiddenBodies() {
        return Stream.of(
                Arguments.of("upstream field name (BR-06)", "name", "hacked"),
                Arguments.of("upstream field weight (BR-06)", "weight", 1),
                Arguments.of("upstream field stats (BR-06)", "stats", List.of(Map.of("name", "hp", "value", 255))),
                Arguments.of("upstream snapshot (BR-06)", "snapshot", Map.of("name", "hacked")),
                Arguments.of("unknown field", "favourite", true));
    }

    @ParameterizedTest(name = "{0} -> 400")
    @MethodSource("forbiddenBodies")
    void should_reject_upstream_or_unknown_fields_with_400(String reason, String field, Object value) {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        Map<String, Object> created = syncNew(user, id, uniqueName(id, "mass"));
        Map<String, Object> body = new LinkedHashMap<>(update(customWithTags("ok"), version(created)));
        body.put(field, value);

        put(user, id, body).then().statusCode(400).contentType(startsWith(PROBLEM_JSON));
        assertThat(get(user, id).jsonPath().getMap("snapshot")).isEqualTo(created.get("snapshot"));
    }

    @Test
    void should_reject_unknown_field_inside_custom_attributes_with_400() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        Map<String, Object> created = syncNew(user, id, uniqueName(id, "inner"));
        Map<String, Object> custom = new LinkedHashMap<>(customWithTags("ok"));
        custom.put("sprite", "https://evil.example/x.png");

        put(user, id, update(custom, version(created))).then().statusCode(400);
    }

    @Test
    void should_reject_body_over_16_kb_with_413() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        Map<String, Object> created = syncNew(user, id, uniqueName(id, "big"));
        String padding = "x".repeat(17 * 1024);
        String body = "{\"customAttributes\":{\"localizedNames\":[],\"tags\":[],\"geo\":{\"region\":\"" + padding
                + "\"}},\"version\":" + version(created) + "}";

        user.writeRaw("PUT", COLLECTION_PATH + "/" + id, body, "application/json")
                .then()
                .statusCode(413)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/payload-too-large"));
    }

    @Test
    void should_reject_non_json_content_type_with_415() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        Map<String, Object> created = syncNew(user, id, uniqueName(id, "plain"));

        user.writeRaw("PUT", COLLECTION_PATH + "/" + id, "tags=a", "text/plain")
                .then()
                .statusCode(415)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/unsupported-media-type"));
        assertThat(version(get(user, id).jsonPath().getMap("$"))).isEqualTo(version(created));
    }

    @Test
    void should_reject_update_without_csrf_token() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        Map<String, Object> created = syncNew(user, id, uniqueName(id, "csrf"));

        user.writeWithCsrfHeader("PUT", COLLECTION_PATH + "/" + id, update(customWithTags("x"), version(created)), null)
                .then()
                .statusCode(403)
                .body("type", equalTo("/problems/csrf-token-invalid"));
    }

    @Test
    void should_reject_anonymous_update_with_401() {
        new Browser()
                .write("PUT", COLLECTION_PATH + "/" + nextPokedexId(), update(customWithTags("x"), 0))
                .then()
                .statusCode(401)
                .body("type", equalTo("/problems/unauthorized"));
    }

    private static Map<String, Object> customWithTagsList(List<String> tags) {
        return custom(List.of(), null, tags);
    }

    private static Map<String, Object> geoWithoutCoordinates(String region, String habitat) {
        Map<String, Object> geo = new LinkedHashMap<>();
        geo.put("region", region);
        geo.put("habitat", habitat);
        geo.put("coordinates", null);
        return geo;
    }
}
