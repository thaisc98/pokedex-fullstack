package com.pokedex.presentation.rest.identity.dto;

import com.pokedex.domain.identity.User;
import io.swagger.v3.oas.annotations.media.Schema;

/** openapi {@code UserDto}: exactly id, email, displayName, role. Never the hash. */
@Schema(name = "UserDto")
public record UserDto(String id, String email, String displayName, String role) {

    public static UserDto from(User user) {
        return new UserDto(
                user.id().value().toString(),
                user.email().value(),
                user.displayName().value(),
                user.role().name());
    }
}
