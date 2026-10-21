package com.pokedex.application.identity.port.out;

import com.pokedex.domain.identity.AccessToken;
import com.pokedex.domain.identity.User;
import java.time.Instant;

/** Signs access tokens (JWT in production). */
public interface TokenIssuer {

    /** A token for {@code user}, valid from {@code issuedAt} until {@code expiresAt}. */
    AccessToken issue(User user, Instant issuedAt, Instant expiresAt);
}
