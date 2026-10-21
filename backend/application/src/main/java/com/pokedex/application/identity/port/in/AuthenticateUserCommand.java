package com.pokedex.application.identity.port.in;

import java.util.Objects;

/** Raw login input. {@link #toString()} never shows the password. */
public record AuthenticateUserCommand(String email, String password) {

    public AuthenticateUserCommand {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(password, "password");
    }

    @Override
    public String toString() {
        return "AuthenticateUserCommand[email=" + email + ", password=***]";
    }
}
