package com.pokedex.application.identity.port.in;

import com.pokedex.domain.identity.AccessToken;
import com.pokedex.domain.identity.User;
import java.util.Objects;

/** A successful login: the user and their token. Safe to print: neither part prints a secret. */
public record AuthenticationResult(User user, AccessToken accessToken) {

    public AuthenticationResult {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(accessToken, "accessToken");
    }
}
