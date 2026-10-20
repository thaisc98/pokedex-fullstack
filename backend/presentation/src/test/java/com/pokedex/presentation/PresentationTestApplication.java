package com.pokedex.presentation;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Root configuration for the presentation module's slice tests. The real {@code @SpringBootApplication}
 * lives in bootstrap, which presentation must not depend on, so {@code @WebMvcTest} needs its own
 * {@code @SpringBootConfiguration} with a component scan of {@code com.pokedex.presentation} to find the
 * controllers it filters on.
 */
@SpringBootApplication
class PresentationTestApplication {}
