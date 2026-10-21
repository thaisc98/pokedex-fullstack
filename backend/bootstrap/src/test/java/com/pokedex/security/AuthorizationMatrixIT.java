package com.pokedex.security;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import io.restassured.RestAssured;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Over-the-wire authorization matrix for the <b>default</b> profile (security.md §3.2, BR-10), with the exact
 * problem {@code type} each denial returns.
 *
 * <p>Deny by default: anything not listed as public is {@code denyAll()}. For an anonymous caller Spring Security
 * turns that into the authentication entry point → <b>401</b> {@code /problems/unauthorized}. A write without a
 * CSRF token is stopped earlier by the CSRF filter → <b>403</b> {@code /problems/csrf-token-invalid}
 * (security.md §4.2.1). {@code /problems/forbidden} (authenticated but not allowed) cannot be produced until
 * identity exists, so it has no row yet.
 *
 * <p>springdoc ({@code /v3/api-docs/**}, {@code /swagger-ui/**}) is enabled in {@code dev}/{@code demo} only
 * (security.md §3.2). In the default profile it falls under "anything else: denied", so 401 like any other
 * non-public route. The demo-profile counterpart lives in {@code OpenApiContractIT}.
 *
 * <p>The public catalog row ({@code GET /api/v1/pokemon} → 200) is covered with a stubbed PokeAPI in
 * {@code ListPokemonIT.should_allow_anonymous_access_without_cookie_or_credentials}; this class makes no upstream
 * call. Its own Spring context is fine.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Security - authorization matrix (default profile)")
class AuthorizationMatrixIT {

    static final String PROBLEM_JSON = "application/problem+json";

    /**
     * Every handler the application serves in the default profile, as {@code "METHOD /path"}. A new controller
     * method without a row here fails {@link #should_have_a_matrix_row_for_every_served_endpoint()}.
     */
    static final Set<String> ENDPOINTS_WITH_A_ROW = Set.of("GET /api/v1/pokemon");

    @Value("${local.server.port}")
    int port;

    @Autowired
    @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port = port;
        RestAssured.basePath = "";
    }

    @ParameterizedTest(name = "anonymous GET {0} -> 401 /problems/unauthorized")
    @CsvSource({
        // not built yet, authenticated per security.md §3.2
        "/api/v1/collection",
        "/api/v1/collection/1",
        "/api/v1/auth/me",
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
                .header("WWW-Authenticate", org.hamcrest.Matchers.nullValue());
    }

    @ParameterizedTest(name = "anonymous {0} {1} without CSRF token -> 403 /problems/csrf-token-invalid")
    @CsvSource({
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
