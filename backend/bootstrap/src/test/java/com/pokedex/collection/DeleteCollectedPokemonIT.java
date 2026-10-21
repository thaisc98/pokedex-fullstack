package com.pokedex.collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Admin delete: {@code DELETE /api/v1/collection/{pokedexId}} (openapi {@code deleteCollectedPokemon}).
 *
 * <p>Traceability: BR-10 and security.md §3.2 (ADMIN 204; USER 403 {@code /problems/forbidden}; anonymous 401),
 * §3 "defence in depth" (the use case checks the role too), §4.2.1 (CSRF required: missing → 403
 * {@code /problems/csrf-token-invalid}). The ADMIN is inserted by the test ({@link #loggedInAdmin()}).
 */
@DisplayName("Collection - delete (ADMIN only)")
class DeleteCollectedPokemonIT extends AbstractCollectionIT {

    @Test
    void should_delete_as_admin_with_204_and_then_return_404() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        syncNew(user, id, uniqueName(id, "del"));
        Browser admin = loggedInAdmin();

        Response response = delete(admin, id);

        response.then().statusCode(204);
        get(admin, id).then().statusCode(404).body("type", equalTo("/problems/not-found"));
        get(user, id).then().statusCode(404);
    }

    @Test
    void should_return_404_when_admin_deletes_a_missing_record() {
        delete(loggedInAdmin(), nextPokedexId())
                .then()
                .statusCode(404)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/not-found"));
    }

    @Test
    void should_forbid_user_from_deleting_and_keep_the_record() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        syncNew(user, id, uniqueName(id, "keep"));

        delete(user, id)
                .then()
                .statusCode(403)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/forbidden"));
        get(user, id).then().statusCode(200);
    }

    @Test
    void should_reject_anonymous_delete_with_401() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        syncNew(user, id, uniqueName(id, "anon"));

        delete(new Browser(), id).then().statusCode(401).body("type", equalTo("/problems/unauthorized"));
        get(user, id).then().statusCode(200);
    }

    @Test
    void should_reject_admin_delete_without_csrf_token() {
        Browser user = loggedInUser();
        int id = nextPokedexId();
        syncNew(user, id, uniqueName(id, "nocsrf"));
        Browser admin = loggedInAdmin();

        admin.writeWithCsrfHeader("DELETE", COLLECTION_PATH + "/" + id, null, null)
                .then()
                .statusCode(403)
                .body("type", equalTo("/problems/csrf-token-invalid"));
        get(user, id).then().statusCode(200);
    }
}
