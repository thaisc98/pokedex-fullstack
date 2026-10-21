package com.pokedex.application.identity.port.in;

/** UC-07: exchange credentials for an access token. */
public interface AuthenticateUserUseCase {

    /**
     * @throws com.pokedex.domain.shared.AuthenticationFailedException for any bad credentials, with one generic
     *     message (401)
     */
    AuthenticationResult authenticate(AuthenticateUserCommand command);
}
