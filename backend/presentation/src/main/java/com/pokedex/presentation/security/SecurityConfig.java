package com.pokedex.presentation.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * API security (security.md §3-4, BR-10):
 *
 * <ul>
 *   <li>Deny by default: only the routes listed here are reachable; every one of them has a row in the
 *       authorization-matrix tests. An anonymous caller on any other route gets 401, an authenticated one 403.
 *   <li>The springdoc routes (API docs and Swagger UI) are readable anonymously only when {@code
 *       pokedex.security.api-docs-public=true}, which only the {@code demo} profile sets. Everywhere else they
 *       are denied like any other unlisted route.
 *   <li>Authentication: the HS256 JWT in the {@code access_token} cookie, validated by the {@code JwtDecoder} bean
 *       (built in bootstrap); its {@code roles} claim becomes {@code ROLE_*} authorities. No Authorization header.
 *       The {@link BearerTokenAuthenticationFilter} is added by hand instead of through {@code oauth2ResourceServer()}:
 *       that DSL exempts every request carrying a bearer token from CSRF, which is only safe for header tokens. Our
 *       token is a cookie the browser sends by itself, so CSRF must apply to it.
 *   <li>Stateless: no HTTP session, no saved request, so no {@code JSESSIONID}.
 *   <li>CSRF stays enabled with {@code csrf.spa()} ({@code XSRF-TOKEN} cookie + {@code X-XSRF-TOKEN} header). The
 *       same {@link CsrfTokenRepository} is a bean so login and logout can reset the token.
 *   <li>No HTTP Basic, form login or Spring logout filter, no CORS (same origin). Errors are
 *       {@code application/problem+json} without a {@code WWW-Authenticate} challenge.
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    /** springdoc's JSON/YAML docs, its swagger-config, and the Swagger UI (custom path + default redirect). */
    static final String[] API_DOCS_PATHS = {"/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"};

    static final String REGISTER = "/api/v1/auth/register";
    static final String LOGIN = "/api/v1/auth/login";
    static final String LOGOUT = "/api/v1/auth/logout";
    static final String ME = "/api/v1/auth/me";
    static final String CSRF = "/api/v1/auth/csrf";

    /** security.md §4.7: API responses are data only; nothing may load, frame or run them. */
    static final String API_CSP = "default-src 'none'; frame-ancestors 'none'";

    /**
     * Swagger UI and the spec ({@code demo} profile only): same-origin scripts (the UI has no inline scripts), inline
     * styles (the UI sets style attributes at runtime), data: images (icons) and same-origin fetches of the spec.
     */
    static final String API_DOCS_CSP = "default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data:; connect-src 'self'; font-src 'self'; frame-ancestors 'none'; "
            + "base-uri 'none'; form-action 'none'";

    static final String PERMISSIONS_POLICY = "camera=(), microphone=(), geolocation=()";
    static final String COLLECTION = "/api/v1/collection";
    static final String COLLECTION_ITEM = "/api/v1/collection/*";

    /**
     * The default StrictHttpFirewall stays as is; URLs it refuses get a 400 validation ProblemDetail (Spring
     * Security uses a {@link RequestRejectedHandler} bean when one exists).
     */
    @Bean
    RequestRejectedHandler requestRejectedHandler() {
        return new ProblemDetailSecurityHandler();
    }

    /** What {@code csrf.spa()} installs: a JavaScript-readable {@code XSRF-TOKEN} cookie. */
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            CsrfTokenRepository csrfTokenRepository,
            JwtDecoder jwtDecoder,
            @Value("${pokedex.security.api-docs-public:false}") boolean apiDocsPublic)
            throws Exception {
        ProblemDetailSecurityHandler problems = new ProblemDetailSecurityHandler();
        http.authorizeHttpRequests(auth -> {
                    // US01 + US02: the catalog list and detail are public
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/pokemon", "/api/v1/pokemon/*")
                            .permitAll();
                    // Identity (security.md §3.2)
                    auth.requestMatchers(HttpMethod.GET, CSRF).permitAll();
                    auth.requestMatchers(HttpMethod.POST, REGISTER, LOGIN).permitAll();
                    auth.requestMatchers(HttpMethod.GET, ME).authenticated();
                    auth.requestMatchers(HttpMethod.POST, LOGOUT).authenticated();
                    // Liveness for Compose/orchestrators: status only (show-details/components: never).
                    // Every other actuator path falls through to denyAll.
                    auth.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll();
                    // Collection (BR-10): shared team dataset; reads, sync and update need USER, delete needs ADMIN.
                    // The ADMIN token carries USER too (Role.impliedRoles); DELETE is checked again in the use case.
                    auth.requestMatchers(HttpMethod.GET, COLLECTION, COLLECTION_ITEM)
                            .hasRole("USER");
                    auth.requestMatchers(HttpMethod.POST, COLLECTION).hasRole("USER");
                    auth.requestMatchers(HttpMethod.PUT, COLLECTION_ITEM).hasRole("USER");
                    auth.requestMatchers(HttpMethod.DELETE, COLLECTION_ITEM).hasRole("ADMIN");
                    if (apiDocsPublic) {
                        auth.requestMatchers(HttpMethod.GET, API_DOCS_PATHS).permitAll();
                    }
                    auth.anyRequest().denyAll();
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
                .addFilter(cookieTokenFilter(jwtDecoder, problems))
                // nosniff, X-Frame-Options DENY and no-cache/no-store stay Spring's defaults; HSTS is only written for
                // secure requests (behind TLS).
                .headers(headers -> headers.addHeaderWriter(contentSecurityPolicy(apiDocsPublic))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicyHeader(permissions -> permissions.policy(PERMISSIONS_POLICY))
                        .httpStrictTransportSecurity(
                                hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(
                        errors -> errors.authenticationEntryPoint(problems).accessDeniedHandler(problems));
        return http.build();
    }

    /** Authenticates a request from its {@code access_token} cookie; an invalid token is a 401 problem. */
    private static BearerTokenAuthenticationFilter cookieTokenFilter(
            JwtDecoder jwtDecoder, ProblemDetailSecurityHandler problems) {
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtDecoder);
        provider.setJwtAuthenticationConverter(rolesClaimConverter());
        BearerTokenAuthenticationConverter fromCookie = new BearerTokenAuthenticationConverter();
        fromCookie.setBearerTokenResolver(new CookieBearerTokenResolver(publicRoutes()));
        BearerTokenAuthenticationFilter filter =
                new BearerTokenAuthenticationFilter(new ProviderManager(provider), fromCookie);
        filter.setAuthenticationFailureHandler(new AuthenticationEntryPointFailureHandler(problems));
        return filter;
    }

    /** The strict API CSP everywhere; the Swagger-friendly one only on the docs routes, and only when they are public. */
    private static HeaderWriter contentSecurityPolicy(boolean apiDocsPublic) {
        ContentSecurityPolicyHeaderWriter api = new ContentSecurityPolicyHeaderWriter(API_CSP);
        if (!apiDocsPublic) {
            return api;
        }
        PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();
        RequestMatcher docs = new OrRequestMatcher(java.util.Arrays.stream(API_DOCS_PATHS)
                .map(path -> (RequestMatcher) paths.matcher(path))
                .toList());
        HeaderWriter docsWriter =
                new DelegatingRequestMatcherHeaderWriter(docs, new ContentSecurityPolicyHeaderWriter(API_DOCS_CSP));
        HeaderWriter apiWriter = new DelegatingRequestMatcherHeaderWriter(new NegatedRequestMatcher(docs), api);
        return (request, response) -> {
            docsWriter.writeHeaders(request, response);
            apiWriter.writeHeaders(request, response);
        };
    }

    /** Routes where the access-token cookie is not even read (a stale cookie must not block them). */
    private static RequestMatcher publicRoutes() {
        PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();
        return new OrRequestMatcher(
                paths.matcher(HttpMethod.POST, REGISTER),
                paths.matcher(HttpMethod.POST, LOGIN),
                paths.matcher(HttpMethod.GET, CSRF));
    }

    private static JwtAuthenticationConverter rolesClaimConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
