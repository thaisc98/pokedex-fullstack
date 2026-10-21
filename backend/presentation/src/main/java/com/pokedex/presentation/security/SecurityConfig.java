package com.pokedex.presentation.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;

/**
 * API security (security.md §3-4, BR-10):
 *
 * <ul>
 *   <li>Deny by default: only the routes listed here are reachable; every one of them has a row in the
 *       authorization-matrix test.
 *   <li>The springdoc routes (API docs and Swagger UI) are readable anonymously only when {@code
 *       pokedex.security.api-docs-public=true}, which only the {@code demo} profile sets. Everywhere else they
 *       are denied like any other unlisted route.
 *   <li>Stateless: no HTTP session, no saved request, so no {@code JSESSIONID}.
 *   <li>CSRF stays enabled, configured for a single-page app ({@code XSRF-TOKEN} cookie + {@code X-XSRF-TOKEN}
 *       header).
 *   <li>No HTTP Basic or form login, no CORS (same origin). Errors are {@code application/problem+json}.
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    /** springdoc's JSON/YAML docs, its swagger-config, and the Swagger UI (custom path + default redirect). */
    static final String[] API_DOCS_PATHS = {"/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"};

    /**
     * The default StrictHttpFirewall stays as is; URLs it refuses get a 400 validation ProblemDetail (Spring
     * Security uses a {@link RequestRejectedHandler} bean when one exists).
     */
    @Bean
    RequestRejectedHandler requestRejectedHandler() {
        return new ProblemDetailSecurityHandler();
    }

    @Bean
    SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http, @Value("${pokedex.security.api-docs-public:false}") boolean apiDocsPublic)
            throws Exception {
        ProblemDetailSecurityHandler problems = new ProblemDetailSecurityHandler();
        http.authorizeHttpRequests(auth -> {
                    // US01 + US02: the catalog list and detail are public
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/pokemon", "/api/v1/pokemon/*")
                            .permitAll();
                    if (apiDocsPublic) {
                        auth.requestMatchers(HttpMethod.GET, API_DOCS_PATHS).permitAll();
                    }
                    auth.anyRequest().denyAll();
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .csrf(csrf -> csrf.spa())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(
                        errors -> errors.authenticationEntryPoint(problems).accessDeniedHandler(problems));
        return http.build();
    }
}
