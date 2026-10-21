package com.pokedex.security;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import com.pokedex.support.AbstractIntegrationIT;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Over-the-wire authorization matrix for the <b>default</b> profile (security.md §3.2, BR-10), with the exact
 * problem {@code type} each denial returns.
 *
 * <ul>
 *   <li>Deny by default: anything not listed is {@code denyAll()}. For an anonymous caller Spring Security turns that
 *       into the authentication entry point → <b>401</b> {@code /problems/unauthorized}; for an authenticated
 *       {@code USER} it is <b>403</b> {@code /problems/forbidden}.
 *   <li>A write without a CSRF token is stopped earlier by the CSRF filter → <b>403</b>
 *       {@code /problems/csrf-token-invalid} (security.md §4.2.1).
 *   <li>springdoc ({@code /v3/api-docs/**}, {@code /swagger-ui/**}) is enabled in {@code dev}/{@code demo} only. In
 *       the default profile it is "anything else": 401. The demo counterpart is {@code OpenApiContractIT}.
 *   <li>The public catalog rows ({@code GET /api/v1/pokemon[/{idOrName}]} → 200) need a stubbed PokeAPI and are
 *       covered by the anonymous-access tests in {@code ListPokemonIT} and {@code GetPokemonDetailIT}.
 *   <li>Collection rows run without PokeAPI: an authorised call either reaches validation (400) or misses a record
 *       that no test syncs (404), which proves it passed authorization without changing data. The ADMIN is inserted
 *       by the test ({@code loggedInAdmin()}: JdbcTemplate + a BCrypt hash computed in the test).
 * </ul>
 */
@DisplayName("Security - authorization matrix (default profile)")
class AuthorizationMatrixIT extends AbstractIntegrationIT {

    /**
     * Every handler the application serves in the default profile, as {@code "METHOD /path"}. A new controller
     * method without a row here fails {@link #should_have_a_matrix_row_for_every_served_endpoint()}.
     */
    static final Set<String> ENDPOINTS_WITH_A_ROW = Set.of(
            // anonymous allowed: ListPokemonIT.should_allow_anonymous_access_without_cookie_or_credentials
            "GET /api/v1/pokemon",
            // anonymous allowed: GetPokemonDetailIT.should_allow_anonymous_access_to_detail
            "GET /api/v1/pokemon/{idOrName}",
            // identity: rows below (anonymous + USER)
            "GET /api/v1/auth/csrf",
            "POST /api/v1/auth/register",
            "POST /api/v1/auth/login",
            "POST /api/v1/auth/logout",
            "GET /api/v1/auth/me",
            // collection: rows below (anonymous + USER + ADMIN)
            "GET /api/v1/collection",
            "POST /api/v1/collection",
            "GET /api/v1/collection/{pokedexId}",
            "PUT /api/v1/collection/{pokedexId}",
            "DELETE /api/v1/collection/{pokedexId}");

    /** A pokedexId no test ever syncs, so authorised calls end in 404 instead of touching data. */
    static final String MISSING = "/api/v1/collection/999999999";

    static final String VALID_UPDATE =
            "{\"customAttributes\":{\"localizedNames\":[],\"geo\":null,\"tags\":[]},\"version\":0}";

    @Autowired
    @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping;

    // ------------------------------------------------------------ ANONYMOUS

    @ParameterizedTest(name = "anonymous GET {0} -> 401 /problems/unauthorized")
    @CsvSource({
        // authenticated per security.md §3.2
        "/api/v1/auth/me",
        // authenticated per security.md §3.2
        "/api/v1/collection",
        "/api/v1/collection/1",
        // springdoc must not be reachable in the default profile
        "/v3/api-docs",
        "/v3/api-docs/swagger-config",
        "/swagger-ui/index.html",
        "/swagger-ui.html",
        // anything else is denied
        "/api/v1/does-not-exist",
        "/",
    })
    void should_deny_anonymous_get_with_unauthorized_problem(String path) {
        given().when()
                .get(path)
                .then()
                .statusCode(401)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/unauthorized"))
                .body("status", equalTo(401))
                .header("WWW-Authenticate", nullValue());
    }

    @ParameterizedTest(name = "anonymous {0} {1} without CSRF token -> 403 /problems/csrf-token-invalid")
    @CsvSource({
        "POST,   /api/v1/auth/register",
        "POST,   /api/v1/auth/login",
        "POST,   /api/v1/auth/logout",
        "POST,   /api/v1/collection",
        "PUT,    /api/v1/collection/1",
        "DELETE, /api/v1/collection/1",
        "POST,   /api/v1/pokemon",
    })
    void should_reject_write_without_csrf_token_with_csrf_problem(String method, String path) {
        given().contentType("application/json")
                .body("{}")
                .when()
                .request(method, path)
                .then()
                .statusCode(403)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/csrf-token-invalid"))
                .body("status", equalTo(403));
    }

    @Test
    void should_let_anonymous_fetch_csrf_token() {
        new Browser().fetchCsrf().then().statusCode(200);
    }

    @ParameterizedTest(name = "anonymous POST {0} with CSRF reaches the endpoint (empty body -> 400)")
    @CsvSource({"/api/v1/auth/register", "/api/v1/auth/login"})
    void should_let_anonymous_reach_register_and_login(String path) {
        // Reaching validation (400) proves the route is public: not 401, not 403.
        new Browser()
                .write("POST", path, "{}")
                .then()
                .statusCode(400)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/validation-error"));
    }

    @Test
    void should_deny_anonymous_logout_with_unauthorized_problem() {
        new Browser().logout().then().statusCode(401).body("type", equalTo("/problems/unauthorized"));
    }

    // ------------------------------------------------------------ USER

    @Test
    void should_let_user_read_own_profile() {
        Browser user = loggedInUser();

        user.get(Browser.ME_PATH).then().statusCode(200).body("role", equalTo("USER"));
    }

    @Test
    void should_let_user_fetch_csrf_token() {
        loggedInUser().fetchCsrf().then().statusCode(200);
    }

    @Test
    void should_let_user_log_out() {
        loggedInUser().logout().then().statusCode(204);
    }

    @ParameterizedTest(name = "USER GET {0} -> 403 /problems/forbidden")
    @CsvSource({"/api/v1/does-not-exist", "/v3/api-docs", "/swagger-ui/index.html"})
    void should_deny_user_anything_not_listed_with_forbidden_problem(String path) {
        loggedInUser()
                .get(path)
                .then()
                .statusCode(403)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/forbidden"));
    }

    // ------------------------------------------------------------ Collection: anonymous, USER, ADMIN

    @ParameterizedTest(name = "anonymous {0} {1} with CSRF -> 401")
    @CsvSource({"POST, /api/v1/collection", "PUT, " + MISSING, "DELETE, " + MISSING})
    void should_deny_anonymous_collection_writes_with_unauthorized_problem(String method, String path) {
        new Browser()
                .write(method, path, "DELETE".equals(method) ? null : VALID_UPDATE)
                .then()
                .statusCode(401)
                .body("type", equalTo("/problems/unauthorized"));
    }

    @Test
    void should_let_user_list_and_get_collection() {
        Browser user = loggedInUser();

        user.get("/api/v1/collection").then().statusCode(200);
        user.get(MISSING).then().statusCode(404).body("type", equalTo("/problems/not-found"));
    }

    @Test
    void should_let_user_sync_and_update_collection() {
        Browser user = loggedInUser();

        user.write("POST", "/api/v1/collection", "{}").then().statusCode(400);
        user.write("PUT", MISSING, VALID_UPDATE).then().statusCode(404);
    }

    @Test
    void should_forbid_user_delete_with_forbidden_problem() {
        loggedInUser()
                .write("DELETE", MISSING, null)
                .then()
                .statusCode(403)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/forbidden"));
    }

    @Test
    void should_let_admin_use_every_collection_endpoint() {
        Browser admin = loggedInAdmin();

        admin.get(Browser.ME_PATH).then().statusCode(200).body("role", equalTo("ADMIN"));
        admin.get("/api/v1/collection").then().statusCode(200);
        admin.get(MISSING).then().statusCode(404);
        admin.write("POST", "/api/v1/collection", "{}").then().statusCode(400);
        admin.write("PUT", MISSING, VALID_UPDATE).then().statusCode(404);
        admin.write("DELETE", MISSING, null).then().statusCode(404).body("type", equalTo("/problems/not-found"));
    }

    // ------------------------------------------------------------ completeness

    @Test
    void should_have_a_matrix_row_for_every_served_endpoint() {
        Set<String> served = new TreeSet<>();
        handlerMapping.getHandlerMethods().keySet().forEach(info -> {
            Set<String> patterns = info.getPatternValues();
            var methods = info.getMethodsCondition().getMethods();
            for (String pattern : patterns) {
                if (pattern.equals("/error")) {
                    continue; // Boot's error controller, not an API endpoint
                }
                if (methods.isEmpty()) {
                    served.add("ANY " + pattern);
                } else {
                    methods.forEach(method -> served.add(method.name() + " " + pattern));
                }
            }
        });

        assertThat(served)
                .as("every served endpoint needs a row in the authorization matrix (AGENTS.md security rules)")
                .isNotEmpty()
                .isSubsetOf(ENDPOINTS_WITH_A_ROW);
    }
}
