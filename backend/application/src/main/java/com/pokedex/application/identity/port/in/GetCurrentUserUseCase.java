package com.pokedex.application.identity.port.in;

import com.pokedex.domain.identity.User;
import com.pokedex.domain.identity.UserId;

/** UC-08: the authenticated principal's account. */
public interface GetCurrentUserUseCase {

    /** @throws com.pokedex.domain.shared.AuthenticationRequiredException when the user no longer exists (401) */
    User currentUser(UserId id);
}
