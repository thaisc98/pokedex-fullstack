package com.pokedex.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokedex.support.AbstractIntegrationIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Passwords and tokens never appear in logs (testing-strategy.md §5 Identity, security.md §4.9 and §6 "No secrets
 * in logs"). The app runs in this JVM, so its console log is captured while a full register → failed login →
 * login → me → logout flow runs.
 */
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("Identity - no credentials in logs")
class CredentialsNotLoggedIT extends AbstractIntegrationIT {

    @Test
    void should_never_log_passwords_or_tokens(CapturedOutput output) {
        // Distinctive values, so a match can only come from logging them.
        String password = "Log-Canary-Password-" + System.nanoTime();
        String wrongPassword = "Log-Canary-Wrong-" + System.nanoTime();
        String email = uniqueEmail("canary");
        Browser browser = new Browser();

        browser.register(email, password, "Canary").then().statusCode(201);
        browser.login(email, wrongPassword).then().statusCode(401);
        browser.login(email, password).then().statusCode(200);
        String token = browser.cookie("access_token");
        browser.get(Browser.ME_PATH).then().statusCode(200);
        browser.logout().then().statusCode(204);

        assertThat(token).isNotBlank();
        assertThat(output.getAll())
                .doesNotContain(password)
                .doesNotContain(wrongPassword)
                .doesNotContain(token)
                .doesNotContain(token.substring(token.lastIndexOf('.') + 1)) // the signature alone
                .doesNotContain(TEST_JWT_SECRET);
    }
}
