package com.pokedex.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import com.pokedex.support.AbstractIntegrationIT;
import io.restassured.response.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code GET /api/v1/auth/csrf} and {@code POST /api/v1/auth/login} (UC-07, openapi {@code getCsrfToken},
 * {@code login}).
 *
 * <p>Traceability: security.md §4.1 (same 401 for unknown email and wrong password; 5 login attempts / minute per
 * IP + email → 429 + {@code Retry-After}), §4.2 (cookie {@code access_token; HttpOnly; Secure; SameSite=Strict;
 * Path=/api; Max-Age=3600}, 60-minute token), §4.2.1 (readable {@code XSRF-TOKEN}, CSRF required on login, login
 * resets the CSRF token, no {@code JSESSIONID}); ADR 0005 / 0008; testing-strategy.md §5 Identity.
 */
@DisplayName("Identity - CSRF token and login")
class LoginIT extends AbstractIntegrationIT {

    static final String PASSWORD = "Squirtle-Wave-Harbor-77";

    // ------------------------------------------------------------ GET /auth/csrf

    @Test
    void should_issue_readable_xsrf_cookie_and_token_descriptor() {
        Browser browser = new Browser();

        Response response = browser.fetchCsrf();

        response.then()
                .statusCode(200)
                .contentType(startsWith("application/json"))
                .body("headerName", equalTo("X-XSRF-TOKEN"))
                .body("token", not(blankOrNullString()));
        assertThat(response.jsonPath().getMap("$").keySet()).containsExactlyInAnyOrder("headerName", "token");
        List<SetCookie> xsrf = SetCookie.all(response, "XSRF-TOKEN");
        assertThat(xsrf).as("Set-Cookie XSRF-TOKEN").hasSize(1);
        assertThat(xsrf.getFirst().value()).isNotBlank();
        assertThat(xsrf.getFirst().has("HttpOnly"))
                .as("XSRF-TOKEN must be readable by JavaScript (not HttpOnly)")
                .isFalse();
    }

    // ------------------------------------------------------------ POST /auth/login

    @Test
    void should_log_in_and_set_access_token_cookie_with_exact_attributes() {
        Browser browser = new Browser();
        String email = uniqueEmail("login");
        Response registered = browser.register(email, PASSWORD, "Login User");
        registered.then().statusCode(201);

        Instant before = Instant.now();
        Response response = browser.login(email, PASSWORD);

        response.then()
                .statusCode(200)
                .contentType(startsWith("application/json"))
                .body("user.email", equalTo(email))
                .body("user.role", equalTo("USER"))
                .body("user.id", equalTo(registered.jsonPath().getString("id")));
        assertThat(response.jsonPath().getMap("$").keySet()).containsExactlyInAnyOrder("user", "expiresAt");
        assertThat(response.jsonPath().getMap("user").keySet())
                .containsExactlyInAnyOrder("id", "email", "displayName", "role");
        Instant expiresAt = Instant.parse(response.jsonPath().getString("expiresAt"));
        assertThat(expiresAt)
                .as("token valid for 60 minutes")
                .isCloseTo(before.plus(60, ChronoUnit.MINUTES), within(2, ChronoUnit.MINUTES));

        List<SetCookie> tokens = SetCookie.all(response, "access_token");
        assertThat(tokens).as("exactly one Set-Cookie access_token").hasSize(1);
        SetCookie cookie = tokens.getFirst();
        assertThat(cookie.value()).as("a JWT").matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
        assertThat(cookie.has("HttpOnly")).as("HttpOnly").isTrue();
        assertThat(cookie.has("Secure")).as("Secure").isTrue();
        assertThat(cookie.attribute("SameSite")).as("SameSite").isEqualToIgnoringCase("Strict");
        assertThat(cookie.attribute("Path")).as("Path").isEqualTo("/api");
        assertThat(cookie.attribute("Max-Age")).as("Max-Age").isEqualTo("3600");
        assertThat(response.asString()).as("the token is only in the cookie").doesNotContain(cookie.value());
    }

    @Test
    void should_log_in_with_email_in_different_case() {
        Browser browser = new Browser();
        String email = uniqueEmail("case");
        browser.register(email, PASSWORD, "Case").then().statusCode(201);

        browser.login(email.toUpperCase(), PASSWORD).then().statusCode(200).body("user.email", equalTo(email));
    }

    @Test
    void should_return_identical_401_for_wrong_password_and_unknown_email() {
        Browser browser = new Browser();
        String email = uniqueEmail("victim");
        browser.register(email, PASSWORD, "Victim").then().statusCode(201);

        Response wrongPassword = browser.login(email, "Not-The-Password-123");
        Response unknownEmail = browser.login(uniqueEmail("nobody"), "Not-The-Password-123");

        wrongPassword
                .then()
                .statusCode(401)
                .contentType(startsWith(PROBLEM_JSON))
                .body("status", equalTo(401))
                .body("type", equalTo("/problems/invalid-credentials"));
        unknownEmail.then().statusCode(401).contentType(startsWith(PROBLEM_JSON));
        assertThat(unknownEmail.asString())
                .as("same body, so the response does not reveal which emails exist (security.md §4.1)")
                .isEqualTo(wrongPassword.asString());
        assertThat(SetCookie.all(wrongPassword, "access_token")).isEmpty();
        assertThat(SetCookie.all(unknownEmail, "access_token")).isEmpty();
    }

    @Test
    void should_reject_login_without_csrf_token() {
        Browser browser = new Browser();
        browser.fetchCsrf();
        String body = "{\"email\":\"" + uniqueEmail("nocsrf") + "\",\"password\":\"" + PASSWORD + "\"}";

        browser.writeWithCsrfHeader("POST", Browser.LOGIN_PATH, body, null)
                .then()
                .statusCode(403)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/csrf-token-invalid"));
    }

    @Test
    void should_reset_csrf_token_on_login_so_the_old_token_is_rejected() {
        Browser browser = new Browser();
        String email = uniqueEmail("rotate");
        browser.register(email, PASSWORD, "Rotate").then().statusCode(201);
        browser.fetchCsrf();
        String oldToken = browser.cookie("XSRF-TOKEN");
        assertThat(oldToken).isNotBlank();

        Response login = browser.login(email, PASSWORD);
        login.then().statusCode(200);

        assertThat(SetCookie.all(login, "XSRF-TOKEN"))
                .as("login resets the CSRF cookie (cleared or replaced), security.md §4.2.1")
                .isNotEmpty()
                .allSatisfy(c ->
                        assertThat(c.deletes() || !c.value().equals(oldToken)).isTrue());
        // The browser applied that Set-Cookie; the SPA still holding the old value is now rejected.
        browser.writeWithCsrfHeader("POST", Browser.LOGOUT_PATH, null, oldToken)
                .then()
                .statusCode(403)
                .body("type", equalTo("/problems/csrf-token-invalid"));
        // After fetching a fresh token, writes work again.
        browser.fetchCsrf();
        assertThat(browser.cookie("XSRF-TOKEN")).isNotEqualTo(oldToken);
        browser.logout().then().statusCode(204);
    }

    // ------------------------------------------------------------ rate limit (security.md §4.1)

    @Test
    void should_return_429_with_retry_after_on_sixth_login_attempt_within_a_minute_for_one_ip_and_email() {
        Browser browser = new Browser();
        String email = uniqueEmail("brute");
        browser.register(email, PASSWORD, "Brute").then().statusCode(201);
        for (int attempt = 1; attempt <= 5; attempt++) {
            browser.login(email, "Wrong-Password-" + attempt)
                    .then()
                    .statusCode(401)
                    .body("type", equalTo("/problems/invalid-credentials"));
        }

        // Even the correct password is throttled now.
        Response sixth = browser.login(email, PASSWORD);

        sixth.then()
                .statusCode(429)
                .contentType(startsWith(PROBLEM_JSON))
                .body("status", equalTo(429))
                .body("type", equalTo("/problems/rate-limited"));
        assertThat(sixth.getHeader("Retry-After"))
                .as("Retry-After header")
                .isNotNull()
                .matches("\\d+");
        assertThat(Integer.parseInt(sixth.getHeader("Retry-After"))).isBetween(0, 60);
        assertThat(SetCookie.all(sixth, "access_token")).isEmpty();

        // The bucket is per IP + email: another account from the same IP is not throttled.
        String other = uniqueEmail("bystander");
        browser.register(other, PASSWORD, "Bystander").then().statusCode(201);
        browser.login(other, PASSWORD).then().statusCode(200);
    }
}
