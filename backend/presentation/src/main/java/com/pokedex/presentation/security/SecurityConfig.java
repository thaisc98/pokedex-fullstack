package com.pokedex.presentation.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;

/**
 * API security (security.md §3-4, BR-10):
 *
 * <ul>
 *   <li>Deny by default: only the routes listed here are reachable; every one of them has a row in the
 *       authorization-matrix test.
 *   <li>Stateless: no HTTP session, no saved request, so no {@code JSESSIONID}.
 *   <li>CSRF stays enabled, configured for a single-page app ({@code XSRF-TOKEN} cookie + {@code X-XSRF-TOKEN}
 *       header).
 *   <li>No HTTP Basic or form login, no CORS (same origin). Errors are {@code application/problem+json}.
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        ProblemDetailSecurityHandler problems = new ProblemDetailSecurityHandler();
        http.authorizeHttpRequests(auth -> auth
                        // US01: the catalog is public
                        .requestMatchers(HttpMethod.GET, "/api/v1/pokemon")
                        .permitAll()
                        .anyRequest()
                        .denyAll())
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
