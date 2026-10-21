package com.pokedex.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import com.pokedex.support.AbstractIntegrationIT;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code POST /api/v1/auth/register} (UC-06, openapi {@code registerUser}).
 *
 * <p>Traceability: BR-09 (unique lowercased email; password 12–64 chars, no composition rules, rejected if on the
 * common-password list); security.md §4.1 (duplicate → 409, 3 registrations / minute per IP → 429 +
 * {@code Retry-After}), §4.2.1 (CSRF required on every write), §4.3 (unknown fields rejected; response lists its
 * fields; no hash, no internal id); testing-strategy.md §5 Identity.
 *
 * <p>Every test uses fresh emails, and the rate-limit buckets are reset before each test (see
 * {@link AbstractIntegrationIT}); no test registers more than three accounts except the 429 test.
 */
@DisplayName("Identity - register")
class RegisterIT extends AbstractIntegrationIT {

    static final String PASSWORD = "Pikachu-Thunder-Meadow-42";

    @Test
    void should_register_user_and_return_user_dto_without_secrets() {
        Browser browser = new Browser();
        String email = uniqueEmail("ash");

        Response response = browser.register(email, PASSWORD, "Ash Ketchum");

        response.then()
                .statusCode(201)
                .contentType(startsWith("application/json"))
                .body("email", equalTo(email))
                .body("displayName", equalTo("Ash Ketchum"))
                .body("role", equalTo("USER"));
        JsonPath json = response.jsonPath();
        assertThat(json.getMap("$").keySet())
                .as("UserDto fields exactly (additionalProperties: false)")
                .containsExactlyInAnyOrder("id", "email", "displayName", "role");
        assertThat(UUID.fromString(json.getString("id"))).as("id is a UUID").isNotNull();
        assertThat(response.asString())
                .as("no password or hash in the response (security.md §4.3)")
                .doesNotContain(PASSWORD)
                .doesNotContain("$2a$")
                .doesNotContain("$2b$")
                .doesNotContain("$2y$")
                .doesNotContain("{bcrypt}")
                .doesNotContainIgnoringCase("password");
        assertThat(SetCookie.all(response, "access_token"))
                .as("registration does not log in")
                .isEmpty();
    }

    @Test
    void should_store_email_trimmed_and_lowercased() {
        Browser browser = new Browser();
        String local = "Misty-" + UUID.randomUUID();
        String messy = "  " + local.toUpperCase() + "@Example.COM  ";

        browser.register(messy, PASSWORD, "Misty")
                .then()
                .statusCode(201)
                .body("email", equalTo(local.toLowerCase() + "@example.com"));
    }

    @Test
    void should_reject_duplicate_email_ignoring_case_with_conflict() {
        Browser browser = new Browser();
        String email = uniqueEmail("brock");
        browser.register(email, PASSWORD, "Brock").then().statusCode(201);

        browser.register(email, PASSWORD, "Brock again")
                .then()
                .statusCode(409)
                .contentType(startsWith(PROBLEM_JSON))
                .body("status", equalTo(409))
                .body("type", equalTo("/problems/email-taken"));
        browser.register(email.toUpperCase(), PASSWORD, "BROCK")
                .then()
                .statusCode(409)
                .contentType(startsWith(PROBLEM_JSON))
                .body("status", equalTo(409))
                .body("type", equalTo("/problems/email-taken"));
    }

    @ParameterizedTest(name = "password ''{0}'' -> 400")
    @ValueSource(
            strings = {
                "elevenchars", // 11 characters
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", // 65 characters
                "password1234", // on the common-password list (testing-strategy §5)
            })
    void should_reject_weak_password_with_validation_problem(String password) {
        new Browser()
                .register(uniqueEmail("weak"), password, "Weak")
                .then()
                .statusCode(400)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/validation-error"))
                .body("errors", not(empty()))
                .body("errors.field", hasItem("password"));
    }

    @Test
    void should_accept_passwords_of_exactly_12_and_64_characters() {
        Browser browser = new Browser();

        browser.register(uniqueEmail("min"), "Zx9-Qw7-Lm5-", "Twelve").then().statusCode(201);
        browser.register(uniqueEmail("max"), "Zx9-Qw7-Lm5-".repeat(5) + "Abcd", "Sixty-four")
                .then()
                .statusCode(201);
    }

    @Test
    void should_reject_unknown_fields_such_as_role_with_bad_request() {
        // Mass assignment (security.md §4.3): a client must not be able to make itself ADMIN.
        Map<String, Object> body =
                Map.of("email", uniqueEmail("mallory"), "password", PASSWORD, "displayName", "M", "role", "ADMIN");

        new Browser()
                .write("POST", Browser.REGISTER_PATH, body)
                .then()
                .statusCode(400)
                .contentType(startsWith(PROBLEM_JSON));
    }

    @Test
    void should_reject_registration_without_csrf_token() {
        Browser browser = new Browser();
        browser.fetchCsrf();
        String body = "{\"email\":\"" + uniqueEmail("nocsrf") + "\",\"password\":\"" + PASSWORD
                + "\",\"displayName\":\"No CSRF\"}";

        browser.writeWithCsrfHeader("POST", Browser.REGISTER_PATH, body, null)
                .then()
                .statusCode(403)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/csrf-token-invalid"));
        browser.writeWithCsrfHeader("POST", Browser.REGISTER_PATH, body, "not-the-token")
                .then()
                .statusCode(403)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/csrf-token-invalid"));
    }

    @Test
    void should_return_429_with_retry_after_on_fourth_registration_within_a_minute_from_one_ip() {
        Browser browser = new Browser();
        for (int i = 1; i <= 3; i++) {
            browser.register(uniqueEmail("burst" + i), PASSWORD, "Burst " + i)
                    .then()
                    .statusCode(201);
        }

        Response fourth = browser.register(uniqueEmail("burst4"), PASSWORD, "Burst 4");

        fourth.then()
                .statusCode(429)
                .contentType(startsWith(PROBLEM_JSON))
                .body("status", equalTo(429))
                .body("type", equalTo("/problems/rate-limited"));
        assertThat(fourth.getHeader("Retry-After"))
                .as("Retry-After header")
                .isNotNull()
                .matches("\\d+");
        assertThat(Integer.parseInt(fourth.getHeader("Retry-After")))
                .as("Retry-After in seconds")
                .isBetween(0, 60);
    }
}
