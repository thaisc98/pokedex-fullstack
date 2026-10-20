package com.pokedex.config;

import com.pokedex.application.shared.SampleGateway;
import com.pokedex.application.shared.SampleService;
import com.pokedex.infrastructure.shared.SampleAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Turns plain use cases and adapters into Spring beans here in bootstrap, so {@code application} needs no
 * {@code @Service} and {@code infrastructure} needs no {@code @Component}. The real wiring lands in P2–P4;
 * this placeholder proves the pattern.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    SampleGateway sampleGateway() {
        return new SampleAdapter();
    }

    @Bean
    SampleService sampleService(SampleGateway sampleGateway) {
        return new SampleService(sampleGateway);
    }
}
