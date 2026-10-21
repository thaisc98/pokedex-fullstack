package com.pokedex.presentation.rest.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/** openapi {@code LoginRequest}. {@link #toString()} never shows the password. */
@Schema(name = "LoginRequest")
public record LoginRequest(
        @Nullable String email,
        @Schema(format = "password") @Nullable String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
