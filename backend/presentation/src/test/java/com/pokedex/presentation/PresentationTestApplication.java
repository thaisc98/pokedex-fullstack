package com.pokedex.presentation;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Root configuration for the presentation module's slice tests. The real {@code @SpringBootApplication}
 * lives in bootstrap, which presentation must not depend on, so {@code @WebMvcTest} needs its own
 * {@code @SpringBootConfiguration} with a component scan of {@code com.pokedex.presentation} to find the
 * controllers it filters on.
 */
@SpringBootApplication
class PresentationTestApplication {

    /**
     * The real HS256 decoder is built in bootstrap from infrastructure. Slice tests authenticate with
     * spring-security-test's {@code jwt()} instead, so any token that reaches this decoder is rejected.
     */
    @Bean
    JwtDecoder rejectingJwtDecoder() {
        return token -> {
            throw new BadJwtException("slice tests do not decode tokens");
        };
    }
}
