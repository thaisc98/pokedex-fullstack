package com.pokedex;

import com.pokedex.support.AbstractIntegrationIT;
import org.junit.jupiter.api.Test;

/**
 * Failsafe integration test (*IT). Proves the full Spring Boot application context starts (with the shared
 * PostgreSQL Testcontainer and test {@code JWT_SECRET} from {@link AbstractIntegrationIT}) and that the Failsafe
 * phase runs on Java 25. It shares its context with the identity and matrix ITs.
 */
class SampleIT extends AbstractIntegrationIT {

    @Test
    void context_loads() {
        // The context starting (bean wiring from UseCaseConfig, component scan) is the assertion.
    }
}
