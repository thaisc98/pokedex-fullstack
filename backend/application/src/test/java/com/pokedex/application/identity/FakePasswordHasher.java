package com.pokedex.application.identity;

import com.pokedex.application.identity.port.out.PasswordHasher;
import com.pokedex.domain.identity.PasswordHash;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic {@link PasswordHasher} shipped in application's test-jar. {@code hash(raw)} is
 * {@code {fake}raw}, so tests can build stored hashes with {@link #hashOf}. Records every call so tests can
 * check that the expensive work happened (or did not). {@link #alwaysMatching()} makes every comparison
 * succeed, to prove the use case does not trust a match against the dummy hash.
 */
public final class FakePasswordHasher implements PasswordHasher {

    /** Never equal to {@code hashOf(anything)}: different prefix. */
    public static final PasswordHash DUMMY = new PasswordHash("{fake-dummy}not-a-real-password");

    private final List<String> hashed = new ArrayList<>();
    private final List<PasswordHash> compared = new ArrayList<>();
    private boolean alwaysMatching;

    public static PasswordHash hashOf(String raw) {
        return new PasswordHash("{fake}" + raw);
    }

    public FakePasswordHasher alwaysMatching() {
        this.alwaysMatching = true;
        return this;
    }

    /** Every raw password passed to {@link #hash}. */
    public List<String> hashed() {
        return List.copyOf(hashed);
    }

    /** Every stored hash passed to {@link #matches}, in call order. */
    public List<PasswordHash> compared() {
        return List.copyOf(compared);
    }

    @Override
    public PasswordHash hash(String rawPassword) {
        hashed.add(rawPassword);
        return hashOf(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
        compared.add(hash);
        return alwaysMatching || hash.equals(hashOf(rawPassword));
    }

    @Override
    public PasswordHash dummyHash() {
        return DUMMY;
    }
}
