package com.pokedex.application.identity;

import com.pokedex.application.identity.port.out.TokenIssuer;
import com.pokedex.domain.identity.AccessToken;
import com.pokedex.domain.identity.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** {@link TokenIssuer} shipped in application's test-jar: returns a readable fake token and records requests. */
public final class FakeTokenIssuer implements TokenIssuer {

    /** One call to {@link #issue}. */
    public record Issued(User user, Instant issuedAt, Instant expiresAt) {}

    private final List<Issued> issued = new ArrayList<>();

    public List<Issued> issued() {
        return List.copyOf(issued);
    }

    @Override
    public AccessToken issue(User user, Instant issuedAt, Instant expiresAt) {
        issued.add(new Issued(user, issuedAt, expiresAt));
        return new AccessToken("fake-token-for-" + user.id().value(), expiresAt);
    }
}
