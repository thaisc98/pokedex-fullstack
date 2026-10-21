package com.pokedex.application.identity.service;

import com.pokedex.application.identity.port.in.GetCurrentUserUseCase;
import com.pokedex.application.identity.port.out.UserRepository;
import com.pokedex.domain.identity.User;
import com.pokedex.domain.identity.UserId;
import com.pokedex.domain.shared.AuthenticationRequiredException;
import java.util.Objects;

/** UC-08. Always reads the repository, so a deleted account with a still-valid token gets 401. */
public final class GetCurrentUserService implements GetCurrentUserUseCase {

    private final UserRepository users;

    public GetCurrentUserService(UserRepository users) {
        this.users = Objects.requireNonNull(users, "users");
    }

    @Override
    public User currentUser(UserId id) {
        Objects.requireNonNull(id, "id");
        return users.findById(id).orElseThrow(AuthenticationRequiredException::new);
    }
}
