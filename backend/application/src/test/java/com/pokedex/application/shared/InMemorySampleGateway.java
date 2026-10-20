package com.pokedex.application.shared;

/**
 * Placeholder in-memory fake shipped in the test-jar so other modules can reuse it (the pattern the
 * real fakes follow in P2). Phase 0 keeps it trivial.
 */
public final class InMemorySampleGateway implements SampleGateway {

    private final int stored;

    public InMemorySampleGateway(int stored) {
        this.stored = stored;
    }

    @Override
    public int load() {
        return stored;
    }
}
