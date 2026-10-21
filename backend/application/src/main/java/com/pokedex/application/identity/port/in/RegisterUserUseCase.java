package com.pokedex.application.identity.port.in;

import com.pokedex.domain.identity.User;

/** UC-06: create a {@code USER} account. */
public interface RegisterUserUseCase {

    /**
     * @throws com.pokedex.domain.shared.InvalidValueException on an invalid email, display name or password (400)
     * @throws com.pokedex.domain.shared.EmailAlreadyRegisteredException when the email is taken (409)
     */
    User register(RegisterUserCommand command);
}
