package com.pokedex.presentation.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;

/**
 * Writes RFC 9457 problems for security failures that happen in the filter chain, before any controller.
 * Bodies are fixed strings: nothing from the request is echoed back. No {@code WWW-Authenticate} challenge,
 * so browsers never show a Basic-auth prompt.
 */
final class ProblemDetailSecurityHandler
        implements AuthenticationEntryPoint, AccessDeniedHandler, RequestRejectedHandler {

    static final String UNAUTHORIZED = "{\"type\":\"/problems/unauthorized\",\"title\":\"Unauthorized\",\"status\":401,"
            + "\"detail\":\"Authentication is required to access this resource.\"}";
    static final String CSRF_INVALID =
            "{\"type\":\"/problems/csrf-token-invalid\",\"title\":\"Invalid CSRF token\",\"status\":403,"
                    + "\"detail\":\"The CSRF token is missing or invalid. Fetch a new token and retry.\"}";
    static final String FORBIDDEN = "{\"type\":\"/problems/forbidden\",\"title\":\"Forbidden\",\"status\":403,"
            + "\"detail\":\"You are not allowed to access this resource.\"}";
    static final String REQUEST_REJECTED =
            "{\"type\":\"/problems/validation-error\",\"title\":\"Validation failed\",\"status\":400,"
                    + "\"detail\":\"One or more fields are invalid.\","
                    + "\"errors\":[{\"field\":\"path\",\"message\":\"contains characters that are not allowed\"}]}";

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        if (accessDeniedException instanceof CsrfException) {
            write(response, HttpServletResponse.SC_FORBIDDEN, CSRF_INVALID);
        } else {
            write(response, HttpServletResponse.SC_FORBIDDEN, FORBIDDEN);
        }
    }

    /**
     * A URL refused by StrictHttpFirewall (for example {@code /pokemon/pika;drop}: Spring MVC would otherwise drop
     * the {@code ;drop} and look up {@code pika}). The rejection reason is not echoed.
     */
    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, RequestRejectedException requestRejectedException)
            throws IOException {
        // The firewall rejects before the security filter chain, so its header writers never run here.
        response.setHeader("Cache-Control", "no-store");
        write(response, HttpServletResponse.SC_BAD_REQUEST, REQUEST_REJECTED);
    }

    private static void write(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        // Bodies are ASCII-only constants, so the bytes are identical in every charset.
        response.getOutputStream().write(body.getBytes(StandardCharsets.US_ASCII));
    }
}
