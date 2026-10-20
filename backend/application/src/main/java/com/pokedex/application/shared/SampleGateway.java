package com.pokedex.application.shared;

/**
 * Placeholder outbound port. Exists only so Phase 0 can prove a use case can be unit-tested with a
 * Mockito mock on Java 25. Replaced by the real ports in P2.
 */
public interface SampleGateway {

    int load();
}
