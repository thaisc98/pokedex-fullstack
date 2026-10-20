package com.pokedex.domain.shared;

/**
 * Placeholder domain value object. It exists only so Phase 0 can prove the build and the test tools
 * (Surefire, JaCoCo, PIT, ArchUnit) run on Java 25 bytecode. It is replaced by the real value objects
 * in P1 and carries no business meaning.
 */
public record Sample(int value) {

    public Sample {
        if (value < 0) {
            throw new IllegalArgumentException("value must not be negative");
        }
    }

    public boolean isPositive() {
        return value > 0;
    }
}
