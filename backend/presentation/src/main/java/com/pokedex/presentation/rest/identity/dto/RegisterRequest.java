package com.pokedex.presentation.rest.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * openapi {@code RegisterRequest}. Unknown fields (e.g. {@code role}) are rejected by the strict JSON mapper; missing
 * fields are reported as validation errors by the controller. {@link #toString()} never shows the password.
 */
@Schema(name = "RegisterRequest")
public record RegisterRequest(
        @Nullable String email,
        @Schema(format = "password") @Nullable String password,
        @Nullable String displayName) {

    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", password=***, displayName=" + displayName + "]";
    }
}
