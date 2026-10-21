package com.pokedex.presentation.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * security.md §4.2: the JWT is read <b>only</b> from the {@code access_token} cookie, never from an
 * {@code Authorization} header. On the public auth endpoints the cookie is ignored, so a stale cookie cannot block
 * logging in again.
 */
class CookieBearerTokenResolverTest {

    private final CookieBearerTokenResolver resolver = new CookieBearerTokenResolver(
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/login"));

    @Test
    void should_read_the_access_token_cookie() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.setCookies(new Cookie("other", "x"), new Cookie("access_token", "a.b.c"));

        assertThat(resolver.resolve(request)).isEqualTo("a.b.c");
    }

    @Test
    void should_ignore_the_authorization_header() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("Authorization", "Bearer a.b.c");

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void should_return_null_without_cookies_or_with_a_blank_cookie() {
        assertThat(resolver.resolve(new MockHttpServletRequest("GET", "/api/v1/auth/me")))
                .isNull();
        MockHttpServletRequest blank = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        blank.setCookies(new Cookie("access_token", ""));
        assertThat(resolver.resolve(blank)).isNull();
    }

    @Test
    void should_ignore_the_cookie_on_public_endpoints() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setCookies(new Cookie("access_token", "expired.token.value"));

        assertThat(resolver.resolve(request)).isNull();
    }
}
