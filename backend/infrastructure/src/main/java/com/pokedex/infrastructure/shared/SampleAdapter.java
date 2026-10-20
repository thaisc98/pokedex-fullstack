package com.pokedex.infrastructure.shared;

import com.pokedex.application.shared.SampleGateway;

/**
 * Placeholder outbound adapter implementing an application port, proving the infrastructure → application
 * dependency direction. Wired as a {@code @Bean} in bootstrap. Replaced by the real adapters in P3.
 */
public final class SampleAdapter implements SampleGateway {

    @Override
    public int load() {
        return 21;
    }
}
