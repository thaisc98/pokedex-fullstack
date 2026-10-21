package com.pokedex.presentation.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Reads the JWT only from the {@code access_token} cookie (security.md §4.2); an {@code Authorization} header is
 * ignored. On public endpoints the cookie is not read at all, so an expired cookie can never block a login.
 */
final class CookieBearerTokenResolver implements BearerTokenResolver {

    private final RequestMatcher ignoreOn;

    CookieBearerTokenResolver(RequestMatcher ignoreOn) {
        this.ignoreOn = Objects.requireNonNull(ignoreOn, "ignoreOn");
    }

    @Override
    public @Nullable String resolve(HttpServletRequest request) {
        if (ignoreOn.matches(request)) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (AccessTokenCookies.NAME.equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
