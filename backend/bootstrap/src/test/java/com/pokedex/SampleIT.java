package com.pokedex;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Failsafe integration test (*IT). Proves the full Spring Boot application context starts and that the
 * Failsafe phase runs on Java 25. The real E2E flows (register → login → sync → …) land in P4.
 */
@SpringBootTest
class SampleIT {

    @Test
    void context_loads() {
        // The context starting (bean wiring from UseCaseConfig, component scan) is the assertion.
    }
}
