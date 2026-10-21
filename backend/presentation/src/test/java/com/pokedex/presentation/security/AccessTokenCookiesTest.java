package com.pokedex.presentation.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** security.md §4.2: {@code access_token; HttpOnly; Secure; SameSite=Strict; Path=/api; Max-Age=3600}. */
class AccessTokenCookiesTest {

    private final AccessTokenCookies cookies = new AccessTokenCookies(Duration.ofMinutes(60));

    @Test
    void should_issue_the_documented_cookie() {
        String header = cookies.issue("a.b.c").toString();

        assertThat(header)
                .startsWith("access_token=a.b.c;")
                .contains("Path=/api", "Max-Age=3600", "Secure", "HttpOnly", "SameSite=Strict");
    }

    @Test
    void should_clear_with_the_same_path_and_max_age_zero() {
        String header = cookies.clear().toString();

        assertThat(header)
                .startsWith("access_token=;")
                .contains("Path=/api", "Max-Age=0", "Secure", "HttpOnly", "SameSite=Strict");
    }
}
