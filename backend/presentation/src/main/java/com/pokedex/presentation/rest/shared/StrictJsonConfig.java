package com.pokedex.presentation.rest.shared;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;

/**
 * Request DTOs reject unknown fields (security.md §4.3, mass assignment): an unknown property such as {@code role}
 * becomes a 400. PokeAPI DTOs opt out explicitly with {@code @JsonIgnoreProperties(ignoreUnknown = true)}.
 */
@Configuration(proxyBeanMethods = false)
public class StrictJsonConfig {

    @Bean
    JsonMapperBuilderCustomizer rejectUnknownProperties() {
        return builder -> builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
