package com.pokedex.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import com.pokedex.support.AbstractIntegrationIT;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code POST /api/v1/auth/logout} (openapi {@code logout}).
 *
 * <p>Traceability: security.md §3.2 (anonymous 401, USER 204), §4.2 (clears the cookie with {@code Max-Age=0};
 * the token itself stays valid until expiry — documented limitation, so it is not tested as revoked),
 * §4.2.1 (CSRF required; logout resets the CSRF token); openapi ({@code access_token=; Max-Age=0; Path=/api}).
 */
@DisplayName("Identity - logout")
class LogoutIT extends AbstractIntegrationIT {

    static final String PASSWORD = "Charmander-Flame-Volcano-9";

    @Test
    void should_log_out_with_204_and_clear_the_access_token_cookie() {
        Browser browser = new Browser();
        browser.registerAndLogin(uniqueEmail("logout"), PASSWORD);

        Response response = browser.logout();

        response.then().statusCode(204);
        assertThat(response.asString()).isEmpty();
        List<SetCookie> cleared = SetCookie.all(response, "access_token");
        assertThat(cleared).as("Set-Cookie access_token clearing the cookie").hasSize(1);
        assertThat(cleared.getFirst().value()).isEmpty();
        assertThat(cleared.getFirst().attribute("Max-Age")).isEqualTo("0");
        assertThat(cleared.getFirst().attribute("Path"))
                .as("same Path as when it was set")
                .isEqualTo("/api");

        // The browser dropped the cookie, so the session is gone for the SPA.
        assertThat(browser.cookie("access_token")).isNull();
        browser.get(Browser.ME_PATH).then().statusCode(401);
    }

    @Test
    void should_reject_logout_without_csrf_token() {
        Browser browser = new Browser();
        browser.registerAndLogin(uniqueEmail("logout-nocsrf"), PASSWORD);

        browser.writeWithCsrfHeader("POST", Browser.LOGOUT_PATH, null, null)
                .then()
                .statusCode(403)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/csrf-token-invalid"));
        assertThat(browser.cookie("access_token")).as("still logged in").isNotNull();
        browser.get(Browser.ME_PATH).then().statusCode(200);
    }

    @Test
    void should_return_401_when_anonymous_user_logs_out() {
        new Browser()
                .logout()
                .then()
                .statusCode(401)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/unauthorized"));
    }

    @Test
    void should_reset_csrf_token_on_logout_so_the_old_token_is_rejected() {
        Browser browser = new Browser();
        String email = uniqueEmail("logout-rotate");
        browser.registerAndLogin(email, PASSWORD);
        browser.fetchCsrf();
        String oldToken = browser.cookie("XSRF-TOKEN");

        Response logout = browser.logout();
        logout.then().statusCode(204);

        assertThat(SetCookie.all(logout, "XSRF-TOKEN"))
                .as("logout resets the CSRF cookie (cleared or replaced), security.md §4.2.1")
                .isNotEmpty()
                .allSatisfy(c ->
                        assertThat(c.deletes() || !c.value().equals(oldToken)).isTrue());
        String loginBody = "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
        browser.writeWithCsrfHeader("POST", Browser.LOGIN_PATH, loginBody, oldToken)
                .then()
                .statusCode(403)
                .body("type", equalTo("/problems/csrf-token-invalid"));
    }
}
