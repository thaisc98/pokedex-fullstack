package com.pokedex.presentation.rest.identity.dto;

import com.pokedex.application.identity.port.in.AuthenticationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** openapi {@code LoginResponse}: the user and the token expiry. The token itself is only in the cookie. */
@Schema(name = "LoginResponse")
public record LoginResponse(UserDto user, Instant expiresAt) {

    public static LoginResponse from(AuthenticationResult result) {
        return new LoginResponse(
                UserDto.from(result.user()), result.accessToken().expiresAt());
    }
}
